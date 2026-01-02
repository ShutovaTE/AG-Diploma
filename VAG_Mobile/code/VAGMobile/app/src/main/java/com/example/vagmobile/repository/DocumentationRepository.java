package com.example.vagmobile.repository;

import com.example.vagmobile.service.MarkdownApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentationRepository {

    public interface DocumentationCallback {
        void onSuccess(String markdownContent);
        void onError(String error);
    }

    public void getMarkdownContent(String url, DocumentationCallback callback) {
        MarkdownApiService service = MarkdownApiService.Factory.create();

        service.getMarkdownContent(url).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Ошибка загрузки: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }
}