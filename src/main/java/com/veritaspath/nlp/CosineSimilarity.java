package com.veritaspath.nlp;

/** Vector math helpers shared by the TF-IDF similarity pipeline. */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double[] normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return vector;
        }
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / norm;
        }
        return result;
    }

    /** Dot product of two already-normalized vectors == cosine similarity, in [0, 1] for non-negative TF-IDF vectors. */
    public static double similarity(double[] a, double[] b) {
        double dot = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
        }
        return Math.max(0.0, Math.min(1.0, dot));
    }

    /**
     * Normalized Levenshtein similarity in [0, 1], used for comparing short
     * quoted spans where word-order matters more than TF-IDF bag-of-words.
     */
    public static double levenshteinSimilarity(String a, String b) {
        String s1 = a == null ? "" : a.trim().toLowerCase();
        String s2 = b == null ? "" : b.trim().toLowerCase();
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
