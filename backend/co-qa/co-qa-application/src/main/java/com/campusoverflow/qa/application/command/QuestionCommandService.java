package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：发布 / 编辑 / 删除问题。 */
@Service
public class QuestionCommandService {

    static final int PLAIN_TEXT_FOR_INDEX = 2000;

    private final QuestionRepository questions;
    private final TagRepository tags;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public QuestionCommandService(QuestionRepository questions, TagRepository tags, IdentityApi identity,
                                  IntegrationEventPublisher events, ContentRenderer renderer,
                                  QuestionListCache listCache, AuditTrail audit, Clock clock) {
        this.questions = questions;
        this.tags = tags;
        this.identity = identity;
        this.events = events;
        this.renderer = renderer;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        Instant now = Instant.now(clock);
        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tagSet = Tags.of(cmd.tags());

        Question question = Question.post(actor.userId(), cmd.courseId(), title, body, tagSet, now);
        questions.save(question);
        tags.adjustUsage(tagSet.names(), 1);

        events.publish(new QuestionPostedEvent(EventIds.next(), now, question.id(), actor.userId(),
                cmd.courseId(), title.value(), renderer.toPlainText(body.value(), PLAIN_TEXT_FOR_INDEX),
                tagSet.names()));
        audit.record("QUESTION_POSTED", actor.userId(), "QUESTION", String.valueOf(question.id()),
                "课程 " + cmd.courseId());
        listCache.invalidateAll();
        return question.id();
    }

    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Question question = load(questionId);
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());
        Instant now = Instant.now(clock);

        List<String> before = question.tags().names();
        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tagSet = Tags.of(cmd.tags());

        question.edit(actor, manages, title, body, tagSet, now);
        questions.save(question);
        adjustTagUsage(before, tagSet.names());

        events.publish(new QuestionEditedEvent(EventIds.next(), now, questionId, title.value(),
                renderer.toPlainText(body.value(), PLAIN_TEXT_FOR_INDEX), tagSet.names()));
        listCache.invalidateAll();
    }

    @Transactional
    public void delete(Actor actor, long questionId) {
        Question question = load(questionId);
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());
        Instant now = Instant.now(clock);

        question.softDelete(actor, manages, now);
        questions.save(question);
        tags.adjustUsage(question.tags().names(), -1);

        events.publish(new ContentDeletedEvent(EventIds.next(), now, "QUESTION", questionId, questionId));
        audit.record("QUESTION_DELETED", actor.userId(), "QUESTION", String.valueOf(questionId),
                "课程 " + question.courseId());
        listCache.invalidateAll();
    }

    private void adjustTagUsage(List<String> before, List<String> after) {
        Set<String> oldTags = new HashSet<>(before);
        Set<String> newTags = new HashSet<>(after);
        List<String> removed = before.stream().filter(tag -> !newTags.contains(tag)).toList();
        List<String> added = after.stream().filter(tag -> !oldTags.contains(tag)).toList();
        if (!removed.isEmpty()) {
            tags.adjustUsage(removed, -1);
        }
        if (!added.isEmpty()) {
            tags.adjustUsage(added, 1);
        }
    }

    private Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1002", "问题不存在：" + questionId));
    }
}
