package com.veritaspath.dto;

import jakarta.validation.constraints.NotBlank;

public class ArticlePreviewRequest {

    @NotBlank(message = "url is required")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
