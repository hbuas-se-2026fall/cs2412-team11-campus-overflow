package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.Guard;

/** Markdown 正文。只保存原文；渲染与 XSS 净化在基础设施层完成。 */
public record MarkdownBody(String value) {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 20000;

    public MarkdownBody {
        value = Guard.requireText(value, MIN_LENGTH, MAX_LENGTH, "QA-1002", "正文");
    }

    public static MarkdownBody ofQuestion(String raw) {
        return new MarkdownBody(Guard.requireText(raw, MIN_LENGTH, MAX_LENGTH, "QA-1002", "问题正文"));
    }

    public static MarkdownBody ofAnswer(String raw) {
        return new MarkdownBody(Guard.requireText(raw, MIN_LENGTH, MAX_LENGTH, "QA-1003", "回答正文"));
    }
}
