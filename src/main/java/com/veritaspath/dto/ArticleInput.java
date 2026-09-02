package com.veritaspath.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * One article to compare: either raw pasted text, or a URL VeritasPath
 * will fetch and extract text from via {@code ArticleFetchService}.
 */
public class ArticleInput {

    @NotBlank(message = "outletName is required")
    private String outletName;

    private String url;

    private String text;

    public ArticleInput() {
    }

    public ArticleInput(String outletName, String url, String text) {
        this.outletName = outletName;
        this.url = url;
        this.text = text;
    }

    public String getOutletName() {
        return outletName;
    }

    public void setOutletName(String outletName) {
        this.outletName = outletName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
