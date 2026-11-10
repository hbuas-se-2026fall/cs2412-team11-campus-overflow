package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        long questionId = resolveQuestionId(cmd.targetType(), cmd.targetId());
        Comment parent = cmd.parentId() == null ? null : comments.findById(cmd.parentId())
                .orElseThrow(() -> new NotFoundException("QA-1006", "父评论不存在：" + cmd.parentId()));
        Instant now = Instant.now(clock);

        Comment comment = Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(), parent,
                cmd.body(), now);
        comments.save(comment);

        events.publish(new CommentPostedEvent(EventIds.next(), now, comment.id(), questionId, comment.authorId(),
                comment.body()));
        return comment.id();
    }

    private long resolveQuestionId(TargetType targetType, long targetId) {
        if (targetType == TargetType.QUESTION) {
            Question question = questions.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA-1002", "问题不存在：" + targetId));
            return question.id();
        }
        Answer answer = answers.findById(targetId)
                .orElseThrow(() -> new NotFoundException("QA-1003", "回答不存在：" + targetId));
        return answer.questionId();
    }
}
