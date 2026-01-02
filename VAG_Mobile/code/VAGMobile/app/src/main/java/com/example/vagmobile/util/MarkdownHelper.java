package com.example.vagmobile.util;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownHelper {

    private static final String TAG = "MarkdownHelper";
    private static final String DOCS_BASE_URL = "https://maryzhminkovskaya.github.io/VAG_Mobile/";

    public static String processMarkdown(String content) {
        return processMarkdown(content, null);
    }

    public static String processMarkdown(String content, String pageUrl) {
        if (content == null)
            return "";

        Log.d(TAG, "Processing markdown content");

        String processed = processImages(content, pageUrl);
        processed = processLinks(processed);

        Log.d(TAG, "Markdown processing completed");
        return processed;
    }

    private static String processImages(String content, String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) {
            return content;
        }

        URI baseUri;
        try {
            baseUri = new URI(pageUrl);
        } catch (URISyntaxException e) {
            return content;
        }

        Pattern pattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);

            if (url.startsWith("http://") || url.startsWith("https://")) {
                continue;
            }

            try {
                URI resolved = baseUri.resolve(url);
                String absoluteUrl = resolved.toString();
                String replacement = "![" + alt + "](" + absoluteUrl + ")";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } catch (IllegalArgumentException e) {

            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String processLinks(String content) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\((/[^)]+)\\)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String text = matcher.group(1);
            String path = matcher.group(2);

            if (path.startsWith("/#") || path.contains("#")) {
                continue;
            }

            String normalizedPath = path;

            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }

            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath + "README.md";
            } else if (!normalizedPath.endsWith(".md")) {
                normalizedPath = normalizedPath + ".md";
            }

            String absoluteUrl = DOCS_BASE_URL + normalizedPath;
            String replacement = "[" + text + "](" + absoluteUrl + ")";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}