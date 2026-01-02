package com.example.vagmobile.service;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface MarkdownApiService {

    @GET
    Call<String> getMarkdownContent(@Url String url);

    class Factory {
        private static MarkdownApiService instance;

        public static MarkdownApiService create() {
            if (instance == null) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .build();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://raw.githubusercontent.com/")
                        .client(client)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                instance = retrofit.create(MarkdownApiService.class);
            }
            return instance;
        }
    }
}