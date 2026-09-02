package com.veritaspath.controller;

import com.veritaspath.dto.ArticlePreviewRequest;
import com.veritaspath.dto.ArticlePreviewResponse;
import com.veritaspath.service.ArticleFetchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the dashboard fetch and preview an article's extracted text before submitting it for comparison. */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleFetchService articleFetchService;

    public ArticleController(ArticleFetchService articleFetchService) {
        this.articleFetchService = articleFetchService;
    }

    @PostMapping("/preview")
    public ArticlePreviewResponse preview(@Valid @RequestBody ArticlePreviewRequest request) {
        String text = articleFetchService.fetchArticleText(request.getUrl());
        return new ArticlePreviewResponse(request.getUrl(), text, text.length());
    }
}
