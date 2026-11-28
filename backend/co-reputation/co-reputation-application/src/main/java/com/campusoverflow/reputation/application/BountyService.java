package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 用例：悬赏的发起、结算、到期退款。积分冻结与悬赏状态在同一事务中变更。 */
@Service
public class BountyService {

    private final BountyRepository bounties;
    private final ReputationAccountRepository accounts;
    private final ReputationLedgerService ledgerService;
    private final QaQueryApi qa;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final AuditTrail audit;
    private final Clock clock;

    public BountyService(BountyRepository bounties, ReputationAccountRepository accounts,
                         ReputationLedgerService ledgerService, QaQueryApi qa, IdentityApi identity,
                         IntegrationEventPublisher events, AuditTrail audit, Clock clock) {
        this.bounties = bounties;
        this.accounts = accounts;
        this.ledgerService = ledgerService;
        this.qa = qa;
        this.identity = identity;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long open(Actor actor, long questionId, int points, int days) {
        QuestionRef question = qa.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("RP-4001", "问题不存在"));
        boolean questionOpen = !question.deleted() && question.acceptedAnswerId() == null;
        boolean managesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        Instant now = clock.instant();
        Bounty bounty = Bounty.open(questionId, questionOpen, actor, managesCourse, points, days, now);
        ReputationAccount sponsor = accounts.findByUserId(actor.userId())
                .orElseThrow(() -> new NotFoundException("RP-4002", "声誉账户不存在"));
        sponsor.freezePoints(points);
        accounts.save(sponsor);
        bounties.save(bounty);
        events.publish(new BountyOpenedEvent(EventIds.next(), now, bounty.id(), questionId, actor.userId(), points,
                bounty.expiresAt()));
        audit.record("BOUNTY_OPEN", actor.userId(), "QUESTION", String.valueOf(questionId), "points=" + points);
        return bounty.id();
    }

    /** 由 AnswerAccepted 事件触发（在处理器事务内）。幂等：悬赏已结束则直接返回。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void settleOnAccept(long questionId, long answerId, long winnerId, String sourceEventId) {
        Optional<Bounty> maybe = bounties.findOpenByQuestion(questionId);
        if (maybe.isEmpty()) {
            return;
        }
        Bounty bounty = maybe.get();
        Instant now = clock.instant();
        ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                .orElseThrow(() -> new NotFoundException("RP-4002", "声誉账户不存在"));
        if (bounty.sponsorId() == winnerId) {
            closeAndRefund(bounty, sponsor, now);
            return;
        }
        bounty.award(answerId, winnerId, now);
        bounties.save(bounty);
        sponsor.spendFrozen(bounty.points());
        accounts.save(sponsor);
        ledgerService.record(winnerId, sourceEventId, ReputationReason.BOUNTY_AWARDED, bounty.points(), "BOUNTY",
                bounty.id());
        events.publish(new BountyAwardedEvent(EventIds.next(), now, bounty.id(), questionId, bounty.sponsorId(),
                winnerId, bounty.points()));
        audit.record("BOUNTY_AWARDED", winnerId, "BOUNTY", String.valueOf(bounty.id()),
                "points=" + bounty.points());
    }

    /** 到期未采纳的悬赏：关闭并退回冻结积分。由定时任务调用。 */
    @Transactional
    public int expireDue() {
        Instant now = clock.instant();
        int closed = 0;
        for (Bounty bounty : bounties.findDue(now, 200)) {
            Optional<ReputationAccount> sponsor = accounts.findByUserId(bounty.sponsorId());
            if (sponsor.isEmpty()) {
                continue;
            }
            closeAndRefund(bounty, sponsor.get(), now);
            closed++;
        }
        return closed;
    }

    @Transactional(readOnly = true)
    public Optional<BountyView> latest(long questionId) {
        return bounties.findLatestByQuestion(questionId)
                .map(b -> new BountyView(b.id(), b.questionId(), b.sponsorId(),
                        identity.findUser(b.sponsorId()).map(UserSummary::displayName).orElse("已注销用户"),
                        b.points(), b.status().name(), b.createdAt(), b.expiresAt(), b.winnerId(), b.answerId()));
    }

    private void closeAndRefund(Bounty bounty, ReputationAccount sponsor, Instant now) {
        bounty.close(now);
        bounties.save(bounty);
        sponsor.refundFrozen(bounty.points());
        accounts.save(sponsor);
        events.publish(new BountyClosedEvent(EventIds.next(), now, bounty.id(), bounty.questionId(),
                bounty.sponsorId(), bounty.points()));
        audit.record("BOUNTY_CLOSED", sponsor.userId(), "BOUNTY", String.valueOf(bounty.id()),
                "refund=" + bounty.points());
    }
}
