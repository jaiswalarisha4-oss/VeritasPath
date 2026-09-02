package com.veritaspath.model;

/** A quoted span extracted from an article, with the sentence it appeared in for context. */
public record Quote(String text, String context) {
}
