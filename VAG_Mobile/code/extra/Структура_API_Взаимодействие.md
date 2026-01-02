# Структура файлов и код взаимодействия по API в мобильном приложении

## Объяснение архитектуры

В мобильном приложении используется паттерн **Repository Pattern** с библиотекой **Retrofit** для работы с REST API.

### Архитектура взаимодействия:

```
Activity/Fragment
    ↓
ViewModel
    ↓
Repository
    ↓
ApiService (Retrofit Interface)
    ↓
ApiClient (Retrofit Instance)
    ↓
HTTP Request → Сервер
```

---

## Структура файлов

### 1. Сетевая часть (Network Layer)

#### `VAGMobile/app/src/main/java/com/example/vagmobile/network/`

**ApiClient.java** — клиент для создания экземпляров Retrofit
- Создает базовый клиент без авторизации
- Создает клиент с авторизацией (добавляет заголовок Authorization)

**ApiService.java** — интерфейс Retrofit, определяющий все API endpoints
- Содержит аннотации `@GET`, `@POST`, `@PUT`, `@DELETE`
- Определяет параметры запросов (`@Path`, `@Query`, `@Body`, `@Header`)

---

### 2. Репозитории (Repository Layer)

#### `VAGMobile/app/src/main/java/com/example/vagmobile/repository/`

**AuthRepository.java** — работа с аутентификацией
- `login()` — вход в систему
- `register()` — регистрация пользователя

**ArtworkRepository.java** — работа с публикациями
- `getArtworks()` — получить список публикаций
- `getArtwork()` — получить публикацию по ID
- `createArtwork()` — создать публикацию (сложный запрос с файлом)
- `likeArtwork()` / `unlikeArtwork()` — лайки
- `addComment()` — добавить комментарий
- `searchArtworks()` — поиск публикаций

**CategoryRepository.java** — работа с категориями
- `getCategories()` — получить все категории
- `getCategory()` — получить категорию по ID
- `createCategory()` — создать категорию (только админ)
- `updateCategory()` — обновить категорию
- `deleteCategory()` — удалить категорию

**UserRepository.java** — работа с пользователями
- `getCurrentUserProfile()` — получить профиль текущего пользователя
- `updateProfile()` — обновить профиль
- `getUserProfile()` — получить профиль пользователя по ID

**AdminArtworkRepository.java** — админские функции
- `getAdminArtworks()` — получить публикации для модерации
- `approveArtwork()` — одобрить публикацию
- `rejectArtwork()` — отклонить публикацию

---

### 3. Модели данных (Model Layer)

#### `VAGMobile/app/src/main/java/com/example/vagmobile/model/`

**ApiResponse.java** — обертка для ответов API
**AuthResponse.java** — ответ при аутентификации
**Artwork.java** — модель публикации
**Category.java** — модель категории
**User.java** — модель пользователя
**Comment.java** — модель комментария

---

## Детальный код взаимодействия

### 1. ApiClient.java

```java
package com.example.vagmobile.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // Базовый URL сервера
    private static final String BASE_URL = "http://192.168.0.40:8080/";
    private static Retrofit retrofit = null;

    // Создает клиент без авторизации
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Создает клиент с авторизацией (добавляет заголовок Authorization)
    public static Retrofit getClientWithAuth(final String authToken) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", authToken)
                            .method(original.method(), original.body());
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
```

**Что делает:**
- Настраивает Retrofit с базовым URL
- Добавляет логирование HTTP-запросов
- Настраивает таймауты
- Добавляет конвертер JSON (Gson)
- Создает клиент с автоматическим добавлением заголовка Authorization

---

### 2. ApiService.java (ключевые методы)

```java
package com.example.vagmobile.network;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ========== ПРОСТЫЕ ЗАПРОСЫ ==========

    // 1. GET запрос без параметров
    @GET("vag/api/mobile/categories")
    Call<Map<String, Object>> getCategories();

    // 2. GET запрос с параметром пути
    @GET("vag/api/mobile/artworks/{id}")
    Call<Map<String, Object>> getArtwork(@Path("id") Long id);

    // 3. GET запрос с query-параметрами
    @GET("vag/api/mobile/artworks")
    Call<Map<String, Object>> getArtworks(
        @Query("page") int page, 
        @Query("size") int size
    );

    // 4. POST запрос с JSON телом (простой)
    @POST("vag/api/mobile/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    // 5. POST запрос без тела, только заголовок авторизации
    @POST("vag/api/mobile/artworks/{id}/like")
    Call<Map<String, Object>> likeArtwork(
        @Header("Authorization") String authHeader, 
        @Path("id") Long id
    );

    // ========== СЛОЖНЫЕ ЗАПРОСЫ ==========

    // 6. POST запрос с multipart/form-data (сложный - включает файл)
    @Multipart
    @POST("vag/api/mobile/artworks/create")
    Call<Map<String, Object>> createArtwork(
        @Header("Authorization") String authHeader,
        @Part("title") RequestBody title,
        @Part("description") RequestBody description,
        @Part("categoryIds") RequestBody categoryIds,
        @Part MultipartBody.Part imageFile
    );

    // 7. PUT запрос с JSON телом
    @PUT("vag/api/mobile/users/profile/update")
    Call<Map<String, Object>> updateProfile(
        @Header("Authorization") String authHeader,
        @Body Map<String, String> profileData
    );
}
```

**Аннотации Retrofit:**
- `@GET`, `@POST`, `@PUT`, `@DELETE` — HTTP методы
- `@Path("id")` — параметр пути (заменяет `{id}` в URL)
- `@Query("page")` — query-параметр (добавляет `?page=0` в URL)
- `@Body` — тело запроса в формате JSON
- `@Header("Authorization")` — заголовок запроса
- `@Multipart` — для отправки multipart/form-data
- `@Part` — часть multipart запроса (текстовое поле или файл)

---

### 3. AuthRepository.java (пример использования)

```java
package com.example.vagmobile.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.network.ApiClient;
import com.example.vagmobile.network.ApiService;
import com.example.vagmobile.util.SharedPreferencesHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private ApiService apiService;
    private Context context;

    public AuthRepository(Context context) {
        this.context = context;
        // Создаем экземпляр ApiService через ApiClient
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    // Метод для входа в систему
    public MutableLiveData<Map<String, Object>> login(String username, String password) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        // Подготавливаем данные для запроса
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        // Выполняем асинхронный запрос
        apiService.login(loginRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                Call<Map<String, Object>> call, 
                Response<Map<String, Object>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    
                    // Обрабатываем успешный ответ
                    if (responseBody.get("success") != null && 
                        (Boolean) responseBody.get("success")) {
                        
                        // Сохраняем токен
                        Object tokenObj = responseBody.get("token");
                        if (tokenObj != null && context != null) {
                            SharedPreferencesHelper prefs = 
                                new SharedPreferencesHelper(context);
                            prefs.saveToken(tokenObj.toString());
                        }
                    }
                    
                    result.setValue(responseBody);
                } else {
                    // Обрабатываем ошибку
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Login failed: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Обрабатываем сетевую ошибку
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }
}
```

**Что происходит:**
1. Создается экземпляр `ApiService` через `ApiClient.getClient()`
2. Подготавливаются данные запроса (Map с username и password)
3. Вызывается метод `login()` из `ApiService`
4. Используется `enqueue()` для асинхронного выполнения
5. В `onResponse()` обрабатывается успешный ответ
6. В `onFailure()` обрабатываются ошибки сети
7. Результат возвращается через `MutableLiveData` для наблюдения в ViewModel

---

### 4. ArtworkRepository.java (пример сложного запроса)

```java
package com.example.vagmobile.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.network.ApiClient;
import com.example.vagmobile.network.ApiService;
import com.example.vagmobile.util.SharedPreferencesHelper;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ArtworkRepository {
    private ApiService apiService;
    private Context context;

    public ArtworkRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    // Получить токен авторизации
    private String getAuthHeader() {
        if (context != null) {
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
            String token = prefs.getToken();
            if (token != null) {
                return "Bearer " + token;
            }
        }
        return null;
    }

    // Создать публикацию (сложный запрос с файлом)
    public MutableLiveData<Map<String, Object>> createArtwork(
        String title, 
        String description, 
        String categoryIdsJson, 
        File imageFile
    ) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        // Создаем клиент с авторизацией
        ApiService authApiService = ApiClient.getClientWithAuth(authHeader)
            .create(ApiService.class);

        // Подготавливаем текстовые поля как RequestBody
        RequestBody titleBody = RequestBody.create(
            okhttp3.MediaType.parse("text/plain"), 
            title
        );
        RequestBody descriptionBody = RequestBody.create(
            okhttp3.MediaType.parse("text/plain"), 
            description
        );
        RequestBody categoryIdsBody = RequestBody.create(
            okhttp3.MediaType.parse("text/plain"), 
            categoryIdsJson
        );

        // Подготавливаем файл изображения
        RequestBody requestFile = RequestBody.create(
            okhttp3.MediaType.parse("image/*"), 
            imageFile
        );
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
            "imageFile", 
            imageFile.getName(), 
            requestFile
        );

        // Выполняем запрос
        authApiService.createArtwork(
            authHeader, 
            titleBody, 
            descriptionBody, 
            categoryIdsBody, 
            imagePart
        ).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                Call<Map<String, Object>> call, 
                Response<Map<String, Object>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to create artwork");
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }
}
```

**Особенности сложного запроса:**
1. Используется `@Multipart` аннотация в `ApiService`
2. Текстовые поля оборачиваются в `RequestBody`
3. Файл оборачивается в `MultipartBody.Part`
4. Все части отправляются вместе в одном запросе
5. Используется клиент с авторизацией (`getClientWithAuth()`)

---

### 5. Пример использования в ViewModel

```java
public class ArtworkViewModel extends ViewModel {
    private ArtworkRepository repository;
    private MutableLiveData<Map<String, Object>> artworksLiveData = 
        new MutableLiveData<>();

    public ArtworkViewModel(Context context) {
        repository = new ArtworkRepository(context);
    }

    public void loadArtworks(int page, int size) {
        // Вызываем метод репозитория
        repository.getArtworks(page, size).observeForever(response -> {
            artworksLiveData.setValue(response);
        });
    }

    public MutableLiveData<Map<String, Object>> getArtworksLiveData() {
        return artworksLiveData;
    }
}
```

---

### 6. Пример использования в Activity

```java
public class ArtworkListActivity extends AppCompatActivity {
    private ArtworkViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ArtworkViewModel.class);
        
        // Наблюдаем за изменениями данных
        viewModel.getArtworksLiveData().observe(this, response -> {
            if (response != null && response.get("success") != null) {
                Boolean success = (Boolean) response.get("success");
                if (success) {
                    // Обновляем UI с полученными данными
                    List<Artwork> artworks = parseArtworks(response);
                    updateRecyclerView(artworks);
                } else {
                    // Показываем ошибку
                    showError((String) response.get("message"));
                }
            }
        });
        
        // Загружаем данные
        viewModel.loadArtworks(0, 20);
    }
}
```

---

## Схема потока данных

```
┌─────────────────┐
│   Activity      │
│   (UI Layer)    │
└────────┬────────┘
         │ вызывает методы
         ▼
┌─────────────────┐
│   ViewModel     │
│  (Logic Layer)  │
└────────┬────────┘
         │ вызывает методы
         ▼
┌─────────────────┐
│   Repository    │
│  (Data Layer)   │
└────────┬────────┘
         │ создает запрос
         ▼
┌─────────────────┐
│   ApiService    │
│  (Interface)    │
└────────┬────────┘
         │ использует
         ▼
┌─────────────────┐
│   ApiClient     │
│ (Retrofit)      │
└────────┬────────┘
         │ отправляет HTTP
         ▼
┌─────────────────┐
│   Сервер        │
│  (CODE_VAG)     │
└─────────────────┘
```

---

## Зависимости (build.gradle)

Для работы с API используются следующие библиотеки:

```gradle
dependencies {
    // Retrofit для HTTP запросов
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp для HTTP клиента
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Gson для JSON сериализации
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

---

## Итоговая структура файлов

```
VAGMobile/app/src/main/java/com/example/vagmobile/
│
├── network/                          # Сетевая часть
│   ├── ApiClient.java               # Клиент Retrofit
│   └── ApiService.java              # Интерфейс API endpoints
│
├── repository/                      # Репозитории
│   ├── AuthRepository.java          # Аутентификация
│   ├── ArtworkRepository.java       # Публикации
│   ├── CategoryRepository.java      # Категории
│   ├── UserRepository.java          # Пользователи
│   └── AdminArtworkRepository.java  # Админ функции
│
├── model/                           # Модели данных
│   ├── ApiResponse.java
│   ├── AuthResponse.java
│   ├── Artwork.java
│   ├── Category.java
│   ├── User.java
│   └── Comment.java
│
├── viewmodel/                       # ViewModel (используют репозитории)
│   ├── ArtworkViewModel.java
│   ├── AuthViewModel.java
│   └── ...
│
└── util/                            # Утилиты
    └── SharedPreferencesHelper.java # Хранение токена
```

---

## Ключевые моменты

1. **ApiClient** — создает и настраивает Retrofit клиент
2. **ApiService** — определяет все API endpoints через аннотации
3. **Repository** — инкапсулирует логику работы с API, обрабатывает ответы
4. **ViewModel** — использует Repository для получения данных
5. **Activity/Fragment** — наблюдает за LiveData из ViewModel и обновляет UI

Все запросы выполняются асинхронно через `enqueue()`, что не блокирует UI поток.

