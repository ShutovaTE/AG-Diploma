package com.example.vagmobile.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.vagmobile.R;
import com.example.vagmobile.model.DocPage;
import com.example.vagmobile.viewmodel.DocumentationViewModel;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.linkify.LinkifyPlugin;

public class DocumentationDetailFragment extends Fragment {

    private static final String ARG_DOC_PAGE = "doc_page";

    private ProgressBar progressBar;
    private TextView contentTextView;
    private DocumentationViewModel viewModel;
    private DocPage currentDocPage;
    private Markwon markwon;

    public static DocumentationDetailFragment newInstance(DocPage docPage) {
        DocumentationDetailFragment fragment = new DocumentationDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOC_PAGE, docPage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documentation_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DocPage docPage = (DocPage) getArguments().getSerializable(ARG_DOC_PAGE);
        if (docPage != null) {
            currentDocPage = docPage;
            TextView titleTextView = view.findViewById(R.id.tv_doc_detail_title);
            contentTextView = view.findViewById(R.id.contentTextView);
            progressBar = view.findViewById(R.id.progressBar);

            titleTextView.setText(docPage.getTitle());

            // Инициализируем Markwon с поддержкой ссылок
            markwon = Markwon.builder(getContext())
                    .usePlugin(LinkifyPlugin.create())
                    .usePlugin(createLinkPlugin())
                    .build();

            setupViewModel();
            loadDocumentationPage(docPage.getRawUrl());
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DocumentationViewModel.class);

        viewModel.getCurrentContent().observe(getViewLifecycleOwner(), markdownContent -> {
            if (markdownContent != null && contentTextView != null && markwon != null) {
                markwon.setMarkdown(contentTextView, markdownContent);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && getContext() != null) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                if (contentTextView != null) {
                    contentTextView.setText("Ошибка загрузки документации:\n" + errorMessage);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (contentTextView != null) {
                contentTextView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void loadDocumentationPage(String url) {
        if (viewModel != null) {
            viewModel.loadMarkdownContent(url);
        }
    }

    private AbstractMarkwonPlugin createLinkPlugin() {
        return new AbstractMarkwonPlugin() {
            @Override
            public void configureConfiguration(MarkwonConfiguration.Builder builder) {
                builder.linkResolver((view, link) -> {
                    // Проверяем, является ли ссылка внутренней ссылкой на документацию
                    DocPage targetPage = findDocPageByLink(link);
                    if (targetPage != null) {
                        // Открываем страницу документации внутри приложения
                        DocumentationDetailFragment detailFragment = DocumentationDetailFragment.newInstance(targetPage);
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, detailFragment)
                                    .addToBackStack("documentation")
                                    .commit();
                        }
                    } else {
                        // Открываем внешнюю ссылку в браузере
                        try {
                            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Не удалось открыть ссылку: " + link, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
    }

    private DocPage findDocPageByLink(String link) {
        if (link == null || viewModel == null || viewModel.docPages == null) {
            return null;
        }

        String lowerLink = link.toLowerCase();

        // Проверяем, содержит ли ссылка ключевые слова, указывающие на внутренние страницы документации
        if (lowerLink.contains("shutova") || lowerLink.contains("zhminkovskaya")) {
            // Ищем соответствующую страницу по ключевым словам
            for (DocPage page : viewModel.docPages) {
                String title = page.getTitle().toLowerCase();
                String url = page.getRawUrl().toLowerCase();

                // Проверяем совпадение по автору и разделу
                if ((lowerLink.contains("shutova") && title.contains("шутова")) ||
                    (lowerLink.contains("zhminkovskaya") && title.contains("жминьковская"))) {

                    // Для главной страницы автора
                    if (lowerLink.contains("shutova") && !lowerLink.contains("guide") && !lowerLink.contains("adding") &&
                        !lowerLink.contains("managing") && !lowerLink.contains("about") && !lowerLink.contains("faq")) {
                        if (title.contains("шутова: главная")) return page;
                    }

                    // Для страниц Шутовой
                    if (lowerLink.contains("shutova")) {
                        if (lowerLink.contains("guide") && !lowerLink.contains("adding") && !lowerLink.contains("managing")) {
                            if (title.contains("руководство по выставкам")) return page;
                        }
                        if (lowerLink.contains("adding")) {
                            if (title.contains("добавление работ")) return page;
                        }
                        if (lowerLink.contains("managing")) {
                            if (title.contains("управление выставками")) return page;
                        }
                        if (lowerLink.contains("about") && !lowerLink.contains("faq")) {
                            if (title.contains("о проекте")) return page;
                        }
                        if (lowerLink.contains("faq")) {
                            if (title.contains("чаВо")) return page;
                        }
                    }

                    // Для главной страницы Жминьковской
                    if (lowerLink.contains("zhminkovskaya") && !lowerLink.contains("publications") &&
                        !lowerLink.contains("creating") && !lowerLink.contains("managing") &&
                        !lowerLink.contains("forms") && !lowerLink.contains("faq")) {
                        if (title.contains("жминьковская: главная")) return page;
                    }

                    // Для страниц Жминьковской
                    if (lowerLink.contains("zhminkovskaya")) {
                        if (lowerLink.contains("publications") && !lowerLink.contains("creating") && !lowerLink.contains("managing")) {
                            if (title.contains("публикации")) return page;
                        }
                        if (lowerLink.contains("creating")) {
                            if (title.contains("создание публикаций")) return page;
                        }
                        if (lowerLink.contains("managing")) {
                            if (title.contains("управление публикациями")) return page;
                        }
                        if (lowerLink.contains("forms")) {
                            if (title.contains("формы публикаций")) return page;
                        }
                        if (lowerLink.contains("faq")) {
                            if (title.contains("чаВо")) return page;
                        }
                    }
                }
            }
        }

        return null;
    }
}