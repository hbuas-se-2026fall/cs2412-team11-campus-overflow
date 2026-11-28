package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 投票与评论的边界及访问器补充测试。 */
class VoteAndCommentEdgeCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    @Test
    void cannotVoteForOwnContent() {
        assertThatThrownBy(() -> Vote.cast(5, 5, TargetType.QUESTION, 10, 1, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void voteValueMustBePlusOrMinusOne() {
        assertThatThrownBy(() -> Vote.cast(2, 1, TargetType.QUESTION, 10, 0, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Vote.cast(2, 1, TargetType.QUESTION, 10, 2, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changeVoteReturnsPreviousValue() {
        Vote vote = Vote.cast(2, 1, TargetType.QUESTION, 10, 1, NOW);
        int previous = vote.change(-1, NOW.plusSeconds(30));
        assertThat(previous).isEqualTo(1);
        assertThat(vote.value()).isEqualTo(-1);
        assertThat(vote.updatedAt()).isAfter(NOW);
        assertThatThrownBy(() -> vote.change(5, NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reconstitutedVoteExposesAllProperties() {
        Vote vote = Vote.reconstitute(null, 2L, TargetType.ANSWER, 50L, -1, NOW);
        vote.assignId(77L);
        assertThat(vote.id()).isEqualTo(77L);
        assertThat(vote.voterId()).isEqualTo(2L);
        assertThat(vote.targetType()).isEqualTo(TargetType.ANSWER);
        assertThat(vote.targetId()).isEqualTo(50L);
        assertThat(vote.value()).isEqualTo(-1);
        assertThat(vote.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void commentReplyToPersistedParent() {
        Comment parent = Comment.post(TargetType.QUESTION, 10, 10, 2, null, "父评论内容", NOW);
        parent.assignId(100);
        Comment reply = Comment.post(TargetType.QUESTION, 10, 10, 3, parent, "子评论内容", NOW);
        assertThat(reply.parentId()).isEqualTo(100L);
        assertThat(reply.targetType()).isEqualTo(TargetType.QUESTION);
        assertThat(reply.targetId()).isEqualTo(10L);
        assertThat(reply.questionId()).isEqualTo(10L);
        assertThat(reply.authorId()).isEqualTo(3L);
        assertThat(reply.body()).isEqualTo("子评论内容");
        assertThat(reply.createdAt()).isEqualTo(NOW);
        assertThat(reply.id()).isNull();
    }

    @Test
    void nestedReplyIsRejected() {
        Comment parent = Comment.post(TargetType.QUESTION, 10, 10, 2, null, "父评论内容", NOW);
        parent.assignId(100);
        Comment reply = Comment.post(TargetType.QUESTION, 10, 10, 3, parent, "子评论内容", NOW);
        reply.assignId(101);
        assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 4, reply, "孙评论内容", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void replyMustBelongToSameTarget() {
        Comment parent = Comment.post(TargetType.QUESTION, 10, 10, 2, null, "父评论内容", NOW);
        parent.assignId(100);
        assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 20, 10, 3, parent, "跨目标回复内容", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void replyToUnsavedParentIsRejected() {
        Comment parent = Comment.post(TargetType.QUESTION, 10, 10, 2, null, "父评论内容", NOW);
        assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 3, parent, "子评论内容", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void commentBodyMustBeOneToTwoThousandChars() {
        assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 2, null, "", NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 2, null, "x".repeat(2001), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Comment.post(null, 10, 10, 2, null, "目标类型为空", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reconstitutedCommentAssignsIdOnlyOnce() {
        Comment comment = Comment.reconstitute(null, TargetType.ANSWER, 50, 10, 2, null, "评论内容", NOW);
        comment.assignId(200);
        assertThat(comment.id()).isEqualTo(200L);
        assertThatThrownBy(() -> comment.assignId(201)).isInstanceOf(IllegalStateException.class);
    }
}
