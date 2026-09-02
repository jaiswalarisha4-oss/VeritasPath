package com.veritaspath.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal TF-IDF implementation over a fixed corpus of documents (here,
 * the sentences of the two articles being compared). Built from scratch
 * rather than pulled from a library so the scoring logic is transparent
 * and easy to explain: term frequency times inverse document frequency,
 * L2-normalized, compared with cosine similarity.
 */
public class TfIdfVectorizer {

    private final List<List<String>> documents; // tokenized documents
    private final Map<String, Integer> vocabulary = new HashMap<>();
    private final double[] idf;

    public TfIdfVectorizer(List<String> rawDocuments) {
        this.documents = new ArrayList<>();
        for (String doc : rawDocuments) {
            documents.add(TextUtils.significantTokens(doc));
        }
        buildVocabulary();
        this.idf = computeIdf();
    }

    private void buildVocabulary() {
        int index = 0;
        for (List<String> doc : documents) {
            for (String term : doc) {
                if (!vocabulary.containsKey(term)) {
                    vocabulary.put(term, index++);
                }
            }
        }
    }

    private double[] computeIdf() {
        int vocabSize = vocabulary.size();
        int[] docFrequency = new int[vocabSize];
        for (List<String> doc : documents) {
            Set<String> seen = new HashSet<>(doc);
            for (String term : seen) {
                docFrequency[vocabulary.get(term)]++;
            }
        }
        int n = documents.size();
        double[] result = new double[vocabSize];
        for (int i = 0; i < vocabSize; i++) {
            // Smoothed IDF: avoids divide-by-zero and dampens terms that
            // appear in every sentence (e.g. the subject of the article).
            result[i] = Math.log((1.0 + n) / (1.0 + docFrequency[i])) + 1.0;
        }
        return result;
    }

    /** Returns the L2-normalized TF-IDF vector for document at {@code index}. */
    public double[] vectorFor(int index) {
        List<String> doc = documents.get(index);
        double[] vector = new double[vocabulary.size()];
        if (doc.isEmpty()) {
            return vector;
        }
        Map<String, Integer> termCounts = new HashMap<>();
        for (String term : doc) {
            termCounts.merge(term, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : termCounts.entrySet()) {
            int idx = vocabulary.get(e.getKey());
            double tf = (double) e.getValue() / doc.size();
            vector[idx] = tf * idf[idx];
        }
        return CosineSimilarity.normalize(vector);
    }

    public int size() {
        return documents.size();
    }

    public int vocabularySize() {
        return vocabulary.size();
    }
}
