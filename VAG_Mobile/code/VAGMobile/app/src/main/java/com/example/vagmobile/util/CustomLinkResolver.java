package com.example.vagmobile.util;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.example.vagmobile.R;
import com.example.vagmobile.model.DocPage;
import com.example.vagmobile.ui.fragment.DocumentationDetailFragment;

public class CustomLinkResolver {

    private final FragmentActivity activity;
    private static final String TAG = "CustomLinkResolver";

    public CustomLinkResolver(FragmentActivity activity) {
        this.activity = activity;
    }

    public void resolveLink(String link) {
        Log.d(TAG, "Processing link: " + link);

        if (link.startsWith("./")) {
            link = "https://raw.githubusercontent.com/MaryZhminkovskaya/VAG_Mobile/Mary/docs/" +
                    link.substring(2) + "README.md";
        } else if (link.startsWith("/")) {
            link = "https://raw.githubusercontent.com/MaryZhminkovskaya/VAG_Mobile/Mary/docs" +
                    link + "README.md";
        }

        if (link.contains("raw.githubusercontent.com") && link.endsWith(".md")) {
            // Это внутренняя ссылка на Markdown страницу
            openInternalDocumentationPage(link);
        } else if (link.startsWith("http") && !link.contains("raw.githubusercontent.com")) {
            // Внешняя ссылка - открываем в браузере
            openExternalLink(link);
        } else if (link.startsWith("#")) {
            // Якорь внутри страницы
            scrollToAnchor(link);
        } else if (link.contains("github.com") && link.contains("/blob/")) {
            // Ссылка на GitHub blob - конвертируем в raw
            String rawUrl = link.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/");
            openInternalDocumentationPage(rawUrl);
        } else {
            // Другие ссылки - пытаемся открыть как внутренние
            String fullLink = "https://raw.githubusercontent.com/MaryZhminkovskaya/VAG_Mobile/Mary/docs/" + link;
            if (!link.endsWith(".md") && !link.contains(".")) {
                fullLink += "/README.md";
            }
            openInternalDocumentationPage(fullLink);
        }
    }

    private void openInternalDocumentationPage(String rawUrl) {
        Log.d(TAG, "Opening internal page: " + rawUrl);

        if (rawUrl.endsWith("/README.mdREADME.md")) {
            rawUrl = rawUrl.replace("/README.mdREADME.md", "/README.md");
        }

        String pageName = extractPageNameFromUrl(rawUrl);

        DocPage newDocPage = new DocPage(pageName, rawUrl);
        DocumentationDetailFragment newFragment = DocumentationDetailFragment.newInstance(newDocPage);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .addToBackStack("documentation")
                .commit();
    }

    private String extractPageNameFromUrl(String url) {
        if (url.contains("/publications/creating/")) {
            return "Создание публикаций";
        } else if (url.contains("/publications/managing/")) {
            return "Управление публикаций";
        } else if (url.contains("/publications/")) {
            return "Публикации";
        } else if (url.contains("/forms/")) {
            return "Формы публикаций";
        } else if (url.contains("/faq/")) {
            return "Частые вопросы";
        } else if (url.contains("/README.md") && !url.contains("/publications/") && !url.contains("/forms/") && !url.contains("/faq/")) {
            return "Главная";
        } else {
            return "Документация";
        }
    }

    private void openExternalLink(String link) {
        Log.d(TAG, "Opening external link: " + link);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        activity.startActivity(intent);
    }

    private void scrollToAnchor(String anchor) {
        Log.d(TAG, "Scrolling to anchor: " + anchor);
    }
}