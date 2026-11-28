package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 回答聚合的编辑、删除与访问器补充测试。 */
class AnswerEdgeCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Actor AUTHOR = new Actor(3, "回答者", Role.STUDENT);
    private static final Actor TEACHER = new Actor(9, "王老师", Role.TEACHER);

    @Test
    void authorCanEditOwnAnswer() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, AUTHOR.userId(), 50);
        a.edit(AUTHOR, false, MarkdownBody.ofAnswer("修改后的回答内容，需要满足最小长度。"), NOW.plusSeconds(5));
        assertThat(a.body().value()).contains("修改后");
        assertThat(a.updatedAt()).isAfter(a.createdAt());
    }

    @Test
    void deletedAnswerRejectsEditEndorseAndDelete() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, AUTHOR.userId(), 50);
        a.softDelete(AUTHOR, false, false, NOW);
        assertThatThrownBy(() -> a.edit(AUTHOR, false, MarkdownBody.ofAnswer("删除后再修改的回答内容"), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> a.endorse(TEACHER, true)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> a.softDelete(TEACHER, true, false, NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void reconstitutedAnswerExposesAllProperties() {
        Answer a = Answer.reconstitute(50L, 10L, 3L, MarkdownBody.ofAnswer("看叔叔节点的颜色和插入位置。"), 4, 9L,
                NOW, NOW.plusSeconds(60), null, 2);
        assertThat(a.id()).isEqualTo(50L);
        assertThat(a.questionId()).isEqualTo(10L);
        assertThat(a.authorId()).isEqualTo(3L);
        assertThat(a.score()).isEqualTo(4);
        assertThat(a.endorsedBy()).isEqualTo(9L);
        assertThat(a.createdAt()).isEqualTo(NOW);
        assertThat(a.deletedAt()).isNull();
        assertThat(a.version()).isEqualTo(2);
        assertThat(a.isDeleted()).isFalse();
    }

    @Test
    void assignIdTwiceIsRejected() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = Answer.submit(q, AUTHOR.userId(), MarkdownBody.ofAnswer("提交后的回答内容，长度足够。"), NOW);
        a.assignId(51);
        assertThatThrownBy(() -> a.assignId(52)).isInstanceOf(IllegalStateException.class);
    }
}
