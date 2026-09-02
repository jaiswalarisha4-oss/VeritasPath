package com.veritaspath.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * Fetches a news article by URL and extracts its body text. Uses a
 * readability-style heuristic rather than a per-outlet scraper: try known
 * article containers first, fall back to "the &lt;p&gt; tags with the most
 * text", which is enough for most news sites without site-specific rules.
 */
@Service
public class ArticleFetchService {

    private static final String[] ARTICLE_SELECTORS = {
            "article", "[itemprop=articleBody]", "div.article-body",
            "div.story-body", "div.entry-content", "main"
    };

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; VeritasPathBot/1.0; +https://github.com/) AppleWebKit/537.36";

    public String fetchArticleText(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
            return extractBody(doc);
        } catch (Exception e) {
            throw new ArticleFetchException("Could not fetch or parse article at " + url + ": " + e.getMessage(), e);
        }
    }

    private String extractBody(Document doc) {
        for (String selector : ARTICLE_SELECTORS) {
            Elements matches = doc.select(selector);
            if (!matches.isEmpty()) {
                String text = joinParagraphs(matches.first());
                if (text.length() > 200) {
                    return text;
                }
            }
        }
        // Fallback: every <p> on the page, which is noisy but workable for
        // simple templates without a dedicated article wrapper.
        StringBuilder sb = new StringBuilder();
        for (Element p : doc.select("p")) {
            sb.append(p.text()).append(" ");
        }
        return sb.toString().trim();
    }

    private String joinParagraphs(Element container) {
        StringBuilder sb = new StringBuilder();
        for (Element p : container.select("p")) {
            sb.append(p.text()).append(" ");
        }
        return sb.toString().trim();
    }

    public static class ArticleFetchException extends RuntimeException {
        public ArticleFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
