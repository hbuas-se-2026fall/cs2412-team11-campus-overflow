package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 问题聚合的边界与访问器补充测试。 */
class QuestionEdgeCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Actor ASKER = new Actor(1, "提问者", Role.STUDENT);

    @Test
    void answerMustNotBeNull() {
        Question q = QuestionTest.persistedQuestion();
        assertThatThrownBy(() -> q.accept(null, ASKER, false)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deletedQuestionRejectsFurtherOperations() {
        Question q = QuestionTest.persistedQuestion();
        q.softDelete(ASKER, false, NOW);
        Answer a = Answer.reconstitute(50L, 10L, 3, MarkdownBody.ofAnswer("已删除的问题不能采纳这个回答"), 0, null,
                NOW, NOW, null, 0);
        assertThatThrownBy(() -> q.accept(a, ASKER, false)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> q.softDelete(ASKER, false, NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(q::ensureOpenForAnswers).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assignIdTwiceIsRejected() {
        Question q = QuestionTest.persistedQuestion();
        assertThatThrownBy(() -> q.assignId(99)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstitutedQuestionExposesAllProperties() {
        Question q = Question.reconstitute(10L, 1L, 100L, new Title("一个足够长的测试问题标题内容"),
                MarkdownBody.ofQuestion("这是问题的正文，长度需要超过十个字符。"), Tags.of(List.of("java")),
                null, 3, 2, 7, NOW, NOW.plusSeconds(10), null, 5);
        assertThat(q.id()).isEqualTo(10L);
        assertThat(q.authorId()).isEqualTo(1L);
        assertThat(q.courseId()).isEqualTo(100L);
        assertThat(q.score()).isEqualTo(3);
        assertThat(q.answerCount()).isEqualTo(2);
        assertThat(q.viewCount()).isEqualTo(7);
        assertThat(q.deletedAt()).isNull();
        assertThat(q.version()).isEqualTo(5);
        assertThat(q.isAccepted(50)).isFalse();
    }

    @Test
    void tagsOfNullIsRejected() {
        assertThatThrownBy(() -> Tags.of(null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void tagNameRejectsNull() {
        assertThatThrownBy(() -> new TagName(null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void titleBoundaries() {
        assertThat(new Title("x".repeat(10)).value()).hasSize(10);
        assertThat(new Title("x".repeat(150)).value()).hasSize(150);
        assertThatThrownBy(() -> new Title("x".repeat(9))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Title("x".repeat(151))).isInstanceOf(BusinessRuleException.class);
    }
}
