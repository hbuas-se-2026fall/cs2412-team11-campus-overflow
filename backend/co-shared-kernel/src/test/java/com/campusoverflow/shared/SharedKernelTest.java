package com.campusoverflow.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.Guard;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import com.campusoverflow.shared.util.UtcTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 共享内核测试：守卫、异常映射、分页、主体与时间工具。 */
class SharedKernelTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");

    @Test
    @DisplayName("Guard：空值、空白、超长均被拒绝，合法值去首尾空白")
    void guardRequireText() {
        assertThatThrownBy(() -> Guard.requireText(null, 1, 5, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("不能为空");
        assertThatThrownBy(() -> Guard.requireText("   ", 1, 5, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Guard.requireText("abcdef", 1, 5, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("长度");
        assertThat(Guard.requireText("  合法值  ", 1, 5, "QA-1001", "标题")).isEqualTo("合法值");
    }

    @Test
    @DisplayName("Guard：引用非空校验")
    void guardRequireNonNull() {
        assertThatThrownBy(() -> Guard.requireNonNull(null, "QA-1006", "评论目标"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("不能为空");
        assertThat(Guard.requireNonNull("值", "QA-1006", "评论目标")).isEqualTo("值");
    }

    @Test
    @DisplayName("异常：错误码与建议状态码")
    void exceptionsCarryCodeAndStatus() {
        BusinessRuleException rule = new BusinessRuleException("QA-1001", "标题过短");
        assertThat(rule.code()).isEqualTo("QA-1001");
        assertThat(rule.suggestedStatus()).isEqualTo(422);
        assertThat(new ConflictException("QA-1011", "重复采纳").suggestedStatus()).isEqualTo(409);
        assertThat(new ForbiddenException("QA-1008", "无权操作").suggestedStatus()).isEqualTo(403);
        assertThat(new NotFoundException("QA-1002", "不存在").suggestedStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("分页请求：页码与大小做边界保护")
    void pageRequestClamps() {
        assertThat(new PageRequest(0, 0)).isEqualTo(new PageRequest(1, 1));
        assertThat(new PageRequest(-3, 999).size()).isEqualTo(PageRequest.MAX_SIZE);
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(40);
    }

    @Test
    @DisplayName("分页结果：空页、映射与总页数")
    void pageResultBehaviour() {
        assertThat(PageResult.empty(2, 10).items()).isEmpty();

        PageResult<Integer> page = new PageResult<>(List.of(1, 2, 3), 21, 1, 10);
        assertThat(page.map(n -> n * 10).items()).containsExactly(10, 20, 30);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(), 0, 1, 0).totalPages()).isZero();
    }

    @Test
    @DisplayName("主体与角色")
    void actorAndRole() {
        Actor student = new Actor(1, "小明", Role.STUDENT);
        Actor teacher = new Actor(9, "王老师", Role.TEACHER);
        Actor admin = new Actor(99, "管理员", Role.ADMIN);

        assertThat(student.isAdmin()).isFalse();
        assertThat(teacher.isTeacherOrAdmin()).isTrue();
        assertThat(admin.isAdmin()).isTrue();
        assertThat(student.is(1)).isTrue();
        assertThat(Role.STUDENT.label()).isEqualTo("学生");
        assertThat(Role.TEACHER.label()).isEqualTo("教师");
    }

    @Test
    @DisplayName("UTC 时间：与本地时区无关的互转")
    void utcTimeRoundTrip() {
        assertThat(UtcTime.toDb(NOW)).isEqualTo(LocalDateTime.of(2026, 10, 8, 6, 0));
        assertThat(UtcTime.fromDb(LocalDateTime.of(2026, 10, 8, 6, 0))).isEqualTo(NOW);
        assertThat(UtcTime.toDb(null)).isNull();
        assertThat(UtcTime.fromDb(null)).isNull();
    }

    @Test
    @DisplayName("事件 ID 唯一")
    void eventIdsAreUnique() {
        assertThat(EventIds.next()).isNotEqualTo(EventIds.next());
    }
}
