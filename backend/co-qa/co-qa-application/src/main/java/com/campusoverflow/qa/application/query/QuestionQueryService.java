package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问答查询用例（读路径）。 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        Optional<PageResult<QuestionSummaryView>> cached = cache.get(courseId, page);
        if (cached.isPresent()) {
            return cached.get();
        }

        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
        Set<Long> authorIds = rows.items().stream().map(QuestionRow::authorId).collect(Collectors.toSet());
        Map<Long, UserSummary> authors = identity.findUsers(authorIds);

        PageResult<QuestionSummaryView> result = rows.map(row -> new QuestionSummaryView(
                row.id(),
                row.title(),
                renderer.toPlainText(row.body(), EXCERPT_LENGTH),
                row.tags(),
                row.courseId(),
                AuthorView.of(row.authorId(), authors.get(row.authorId())),
                row.score(),
                row.answerCount(),
                row.viewCount(),
                row.acceptedAnswerId() != null,
                row.createdAt()));

        cache.put(courseId, page, result);
        return result;
    }

    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow row = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1002", "问题不存在：" + questionId));
        viewCounter.increment(questionId);

        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        Set<Long> userIds = new HashSet<>();
        userIds.add(row.authorId());
        for (AnswerRow answer : answerRows) {
            userIds.add(answer.authorId());
            if (answer.endorsedBy() != null) {
                userIds.add(answer.endorsedBy());
            }
        }
        for (CommentRow comment : commentRows) {
            userIds.add(comment.authorId());
        }
        Map<Long, UserSummary> users = identity.findUsers(userIds);

        Map<String, Integer> myVotes = actorOrNull == null
                ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId,
                        answerRows.stream().map(AnswerRow::id).toList());

        boolean manages = actorOrNull != null
                && identity.canManageCourse(actorOrNull.userId(), row.courseId());
        boolean isAsker = actorOrNull != null && actorOrNull.is(row.authorId());
        PermissionsView permissions = actorOrNull == null
                ? PermissionsView.NONE
                : new PermissionsView(isAsker, isAsker || manages, isAsker || manages, manages);

        List<CommentView> questionComments = commentRows.stream()
                .filter(comment -> "QUESTION".equals(comment.targetType()) && comment.targetId() == row.id())
                .sorted(Comparator.comparing(CommentRow::createdAt))
                .map(comment -> toCommentView(comment, users))
                .toList();

        Map<Long, List<CommentView>> commentsByAnswer = commentRows.stream()
                .filter(comment -> "ANSWER".equals(comment.targetType()))
                .sorted(Comparator.comparing(CommentRow::createdAt))
                .collect(Collectors.groupingBy(CommentRow::targetId,
                        Collectors.mapping(comment -> toCommentView(comment, users), Collectors.toList())));

        Long acceptedAnswerId = row.acceptedAnswerId();
        List<AnswerView> answerViews = answerRows.stream()
                .sorted(Comparator.comparingInt(AnswerRow::score).reversed()
                        .thenComparing(AnswerRow::createdAt))
                .map(answer -> new AnswerView(
                        answer.id(),
                        answer.body(),
                        renderer.toSafeHtml(answer.body()),
                        AuthorView.of(answer.authorId(), users.get(answer.authorId())),
                        answer.score(),
                        acceptedAnswerId != null && acceptedAnswerId == answer.id(),
                        answer.endorsedBy() == null
                                ? null
                                : AuthorView.of(answer.endorsedBy(), users.get(answer.endorsedBy())),
                        myVotes.getOrDefault("ANSWER:" + answer.id(), 0),
                        actorOrNull != null && (actorOrNull.is(answer.authorId()) || manages),
                        answer.createdAt(),
                        answer.updatedAt(),
                        commentsByAnswer.getOrDefault(answer.id(), List.of())))
                .toList();

        CourseRefView course = row.courseId() == null
                ? null
                : identity.findCourse(row.courseId())
                        .map(found -> new CourseRefView(found.id(), found.code(), found.name()))
                        .orElse(null);

        return new QuestionDetailView(
                row.id(),
                row.title(),
                row.body(),
                renderer.toSafeHtml(row.body()),
                row.tags(),
                course,
                AuthorView.of(row.authorId(), users.get(row.authorId())),
                row.score(),
                row.answerCount(),
                row.viewCount(),
                row.acceptedAnswerId(),
                myVotes.getOrDefault("QUESTION:" + row.id(), 0),
                row.createdAt(),
                row.updatedAt(),
                permissions,
                questionComments,
                answerViews);
    }

    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(limit);
    }

    private CommentView toCommentView(CommentRow comment, Map<Long, UserSummary> users) {
        return new CommentView(comment.id(), AuthorView.of(comment.authorId(), users.get(comment.authorId())),
                comment.parentId(), comment.body(), comment.createdAt());
    }
}
