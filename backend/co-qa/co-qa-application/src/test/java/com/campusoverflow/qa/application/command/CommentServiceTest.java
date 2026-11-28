package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 评论用例测试：目标解析（问题/回答）、二级回复、事件发布与缺失目标的错误处理。 */
class CommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryComments comments = new InMemoryComments();
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private CommentService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, questions, answers, events, clock);
        Question q = questions.save(Question.post(ming.userId(), 100L, new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个反例。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        questionId = q.id();
        Answer a = answers.save(Answer.submit(q, hong.userId(), MarkdownBody.ofAnswer("关键在于黑高这个不变量。"), NOW));
        answerId = a.id();
    }

    @Test
    @DisplayName("评论问题：questionId 取自问题本身，并发布 CommentPosted 事件")
    void commentOnQuestionPublishesEvent() {
        long commentId = service.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, null, "补充一句"));

        assertThat(commentId).isPositive();
        assertThat(comments.findById(commentId)).isPresent()
                .get().extracting(Comment::questionId).isEqualTo(questionId);
        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).questionId()).isEqualTo(questionId);
        assertThat(posted.get(0).authorId()).isEqualTo(ming.userId());
    }

    @Test
    @DisplayName("评论回答：questionId 由回答反查得到")
    void commentOnAnswerResolvesQuestionId() {
        long commentId = service.post(hong, new PostCommentCommand(TargetType.ANSWER, answerId, null, "同意这个解释"));

        assertThat(comments.findById(commentId)).isPresent()
                .get().extracting(Comment::questionId).isEqualTo(questionId);
    }

    @Test
    @DisplayName("二级回复：parentId 被正确记录")
    void replyToCommentStoresParentId() {
        long parentId = service.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, null, "父评论内容"));
        long replyId = service.post(hong, new PostCommentCommand(TargetType.QUESTION, questionId, parentId, "回复内容"));

        assertThat(comments.findById(replyId)).isPresent()
                .get().extracting(Comment::parentId).isEqualTo(parentId);
    }

    @Test
    @DisplayName("目标不存在时抛出 NotFound")
    void missingTargetsAreNotFound() {
        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.QUESTION, 999L, null, "目标问题不存在")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.ANSWER, 999L, null, "目标回答不存在")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.QUESTION, questionId, 999L, "父评论不存在")))
                .isInstanceOf(NotFoundException.class);
    }
}
