package com.campusoverflow.qa.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.campusoverflow.qa.application.command.QuestionCommandService;
import com.campusoverflow.qa.application.query.AuthorView;
import com.campusoverflow.qa.application.query.QuestionQueryService;
import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 契约测试：锁定问题列表接口的响应结构。
 * 任何字段改名、类型变化都会让该测试失败，防止前端与后端契约漂移。
 */
class QuestionApiContractTest {

    private final QuestionCommandService commands = mock(QuestionCommandService.class);
    private final QuestionQueryService queries = mock(QuestionQueryService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new QuestionController(commands, queries)).build();
    }

    @Test
    @DisplayName("GET /api/v1/questions 返回 items/total/page/size 四个顶层字段")
    void listContract() throws Exception {
        QuestionSummaryView item = new QuestionSummaryView(
                1L, "契约测试标题", "摘要", List.of("java"), null,
                new AuthorView(1L, "曹文", "STUDENT", false),
                0, 0, 0, false, Instant.parse("2026-11-20T00:00:00Z"));
        given(queries.list(any(), any())).willReturn(new PageResult<>(List.of(item), 1L, 1, 20));

        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].title").isString())
                .andExpect(jsonPath("$.items[0].tags").isArray())
                .andExpect(jsonPath("$.items[0].score").isNumber())
                .andExpect(jsonPath("$.items[0].answerCount").isNumber());
    }
}