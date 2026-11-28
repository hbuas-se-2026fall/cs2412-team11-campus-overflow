package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingTags;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubRenderer;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.ForbiddenException;
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

/** 问题命令用例测试：发布 / 编辑 / 删除的编排逻辑，包括标签计数、事件、审计与缓存失效。 */
class QuestionCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final RecordingTags tags = new RecordingTags();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubRenderer renderer = new StubRenderer();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER)
            .withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    private QuestionCommandService service;

    @BeforeEach
    void setUp() {
        service = new QuestionCommandService(questions, tags, identity, events, renderer, cache, audit, clock);
    }

    private long postQuestion() {
        return service.post(ming, new PostQuestionCommand(100L, "红黑树插入后为什么必须旋转？",
                "希望有直观解释，最好能给出一个反例。", List.of("数据结构", "红黑树")));
    }

    @Test
    @DisplayName("发布问题：落库、标签 +1、发布事件、写审计、失效列表缓存")
    void postPublishesEventAndBumpsTagUsage() {
        long id = postQuestion();

        assertThat(id).isPositive();
        assertThat(tags.deltas).containsEntry("数据结构", 1).containsEntry("红黑树", 1);
        List<QuestionPostedEvent> posted = events.ofType(QuestionPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).questionId()).isEqualTo(id);
        assertThat(posted.get(0).authorId()).isEqualTo(ming.userId());
        assertThat(posted.get(0).courseId()).isEqualTo(100L);
        assertThat(posted.get(0).plainText()).contains("直观解释");
        assertThat(audit.actions).contains("QUESTION_POSTED");
        assertThat(cache.invalidations).isEqualTo(1);
    }

    @Test
    @DisplayName("编辑问题：按新旧标签差量调整使用量，并发布编辑事件")
    void editRecomputesTagUsageByDifference() {
        long id = postQuestion();
        tags.deltas.clear();

        service.edit(ming, id, new EditQuestionCommand("红黑树插入后旋转的判定规则？",
                "更新后的正文，补充了叔叔节点颜色的判断。", List.of("红黑树", "算法")));

        assertThat(tags.deltas).containsEntry("数据结构", -1).containsEntry("算法", 1);
        List<QuestionEditedEvent> edited = events.ofType(QuestionEditedEvent.class);
        assertThat(edited).hasSize(1);
        assertThat(edited.get(0).questionId()).isEqualTo(id);
        assertThat(cache.invalidations).isEqualTo(2);
    }

    @Test
    @DisplayName("非作者且非课程管理者不能编辑")
    void strangerCannotEdit() {
        long id = postQuestion();
        assertThatThrownBy(() -> service.edit(hong, id, new EditQuestionCommand(
                "路人试图修改的问题标题内容", "路人试图修改的正文内容，长度足够。", List.of("java"))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("删除问题：标签 -1，发布内容删除事件并写审计")
    void deleteReleasesTagUsage() {
        long id = postQuestion();
        tags.deltas.clear();

        service.delete(ming, id);

        assertThat(tags.deltas).containsEntry("数据结构", -1).containsEntry("红黑树", -1);
        assertThat(events.ofType(ContentDeletedEvent.class)).hasSize(1);
        assertThat(audit.actions).contains("QUESTION_DELETED");
    }

    @Test
    @DisplayName("课程管理者可以删除已有回答的问题")
    void managerCanDeleteAnsweredQuestion() {
        Question answered = Question.reconstitute(500L, ming.userId(), 100L, new Title("一个已经有回答的问题标题"),
                MarkdownBody.ofQuestion("这个问题已经有一个回答，所以提问者不能删除。"), Tags.of(List.of("java")),
                null, 0, 1, 0, NOW, NOW, null, 0);
        questions.save(answered);

        service.delete(teacher, 500L);

        assertThat(answered.isDeleted()).isTrue();
        assertThat(tags.deltas).containsEntry("java", -1);
    }

    @Test
    @DisplayName("编辑或删除不存在的问题抛出 NotFound")
    void editingMissingQuestionIsNotFound() {
        assertThatThrownBy(() -> service.edit(ming, 999L, new EditQuestionCommand(
                "一个不存在的目标问题标题", "一个不存在的目标问题正文内容。", List.of("java"))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(ming, 999L)).isInstanceOf(NotFoundException.class);
    }
}
