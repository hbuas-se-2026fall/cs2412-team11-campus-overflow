package com.campusoverflow.qa.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubReadModel;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubRenderer;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 问答查询用例测试：列表映射与缓存、详情装配（权限/投票/评论分组）、热门标签与缺失问题。 */
class QuestionQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");

    private final StubReadModel readModel = new StubReadModel();
    private final StubViewCounter viewCounter = new StubViewCounter();
    private final CountingCache cache = new CountingCache();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER)
            .withCourseManager(9);
    private final StubRenderer renderer = new StubRenderer();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);

    private QuestionQueryService service;

    @BeforeEach
    void setUp() {
        service = new QuestionQueryService(readModel, identity, renderer, viewCounter, cache);
    }

    private static QuestionRow questionRow() {
        return new QuestionRow(10L, 1L, 100L, "红黑树插入后为什么必须旋转？", "变色为什么不够？希望有直观解释。",
                List.of("数据结构"), 50L, 3, 2, 7, NOW, NOW);
    }

    @Test
    @DisplayName("列表：行投影转换为视图，摘要经过渲染端口")
    void listMapsRowsToViews() {
        readModel.questionsPage = new PageResult<>(List.of(questionRow()), 1, 1, 20);

        PageResult<QuestionSummaryView> page = service.list(100L, new PageRequest(1, 20));

        assertThat(page.total()).isEqualTo(1);
        QuestionSummaryView item = page.items().get(0);
        assertThat(item.id()).isEqualTo(10L);
        assertThat(item.excerpt()).isEqualTo("变色为什么不够？希望有直观解释。");
        assertThat(item.author().displayName()).isEqualTo("小明");
        assertThat(item.accepted()).isTrue();
    }

    @Test
    @DisplayName("列表：命中缓存时直接返回缓存结果，不访问读模型")
    void listReturnsCachedPage() {
        PageResult<QuestionSummaryView> cached = new PageResult<>(List.of(), 0, 2, 20);
        cache.preloaded = cached;

        assertThat(service.list(100L, new PageRequest(2, 20))).isSameAs(cached);
    }

    @Test
    @DisplayName("详情：装配权限、投票、课程与评论分组")
    void detailAssemblesFullView() {
        readModel.question = questionRow();
        readModel.answers = List.of(
                new AnswerRow(60L, 2L, "低分回答", 1, null, NOW, NOW),
                new AnswerRow(50L, 2L, "高分回答", 5, 9L, NOW, NOW));
        readModel.comments = List.of(
                new CommentRow(1L, "QUESTION", 10L, 1L, null, "问题评论", NOW),
                new CommentRow(2L, "ANSWER", 50L, 2L, 1L, "回答评论", NOW));
        readModel.myVotes = Map.of("QUESTION:10", 1, "ANSWER:50", -1);

        QuestionDetailView view = service.detail(10L, ming);

        assertThat(view.course()).isNotNull();
        assertThat(view.course().code()).isEqualTo("CS2001");
        assertThat(view.permissions()).isEqualTo(new PermissionsView(true, true, true, false));
        assertThat(view.myVote()).isEqualTo(1);
        assertThat(view.comments()).hasSize(1);
        assertThat(view.answers()).hasSize(2);
        assertThat(view.answers().get(0).id()).isEqualTo(50L);
        assertThat(view.answers().get(0).accepted()).isTrue();
        assertThat(view.answers().get(0).myVote()).isEqualTo(-1);
        assertThat(view.answers().get(0).comments()).hasSize(1);
        assertThat(view.answers().get(0).endorsedBy()).isNotNull();
        assertThat(view.answers().get(1).canEdit()).isFalse();
        assertThat(viewCounter.incremented).containsExactly(10L);
    }

    @Test
    @DisplayName("详情：匿名访问没有权限也没有投票")
    void detailWithoutActorHasNoPermissions() {
        readModel.question = questionRow();

        QuestionDetailView view = service.detail(10L, null);

        assertThat(view.permissions()).isEqualTo(PermissionsView.NONE);
        assertThat(view.myVote()).isZero();
        assertThat(view.answers()).isEmpty();
    }

    @Test
    @DisplayName("详情：问题不存在时抛出 NotFound")
    void detailMissingQuestionIsNotFound() {
        assertThatThrownBy(() -> service.detail(999L, ming)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("热门标签：直接委托读模型")
    void popularTagsDelegatesToReadModel() {
        readModel.tags = List.of(new TagView("java", 5));

        assertThat(service.popularTags(10)).containsExactly(new TagView("java", 5));
    }
}
