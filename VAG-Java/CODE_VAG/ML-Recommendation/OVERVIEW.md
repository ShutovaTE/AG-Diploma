# Обзор модуля рекомендаций

## 📦 Созданные файлы

### Java-код (CODE_VAG)
```
CODE_VAG/src/main/java/com/example/vag/recommendation/
├── dto/
│   └── RecommendationDTO.java              ✅ DTO для данных рекомендаций
├── service/
│   ├── RecommendationService.java          ✅ Интерфейс сервиса
│   └── RecommendationServiceImpl.java      ✅ Реализация (ProcessBuilder → Python)
├── controller/
│   └── RecommendationController.java       ✅ REST API (закомментирован)
├── recommendation-context.xml              ✅ Spring XML конфигурация (закомментирована)
├── recommendation.properties               ✅ Настраиваемые параметры
└── README.md                               ✅ Документация модуля

CODE_VAG/src/test/java/com/example/vag/recommendation/
└── service/
    └── RecommendationServiceImplTest.java  ✅ Юнит-тесты
```

### Python-скрипт (ML-Recommendation)
```
ML-Recommendation/
├── recommendation_engine.py                ✅ Главный скрипт рекомендаций
├── Prototype RS.py                         ✅ Оригинальный прототип (Colab)
└── INTEGRATION_GUIDE.md                    ✅ Пошаговая инструкция
```

### Зависимости (pom.xml)
```xml
<!-- Добавлено в pom.xml -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.13.3</version>
</dependency>

<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

---

## 🎯 Что делает модуль

### Гибридная система рекомендаций

1. **Контентная фильтрация** — находит похожие работы по:
   - Имени автора
   - Категориям (жанрам)
   - TF-IDF векторизация + косинусное сходство

2. **Коллаборативная фильтрация (SVD)** — предсказывает интерес на основе:
   - Лайков пользователей
   - Комментариев
   - Матричная факторизация через SGD

3. **Гибридный подход** — комбинирует оба метода:
   - `alpha=0.6` — 60% коллаборативная, 40% контентная
   - Настраиваемый вес

---

## 🔌 Архитектура интеграции

```
┌─────────────────────────────────────────────────────┐
│                  Java Application                    │
│                                                      │
│  HomeController/ArtworkController                    │
│       │                                              │
│       ▼                                              │
│  RecommendationService (интерфейс)                   │
│       │                                              │
│       ▼                                              │
│  RecommendationServiceImpl                           │
│       │                                              │
│       │  ProcessBuilder                              │
│       │  "python recommendation_engine.py            │
│       │   --user_id 1"                               │
│       ▼                                              │
│  JSON Parser (Jackson)                               │
│       │                                              │
│       ▼                                              │
│  List<RecommendationDTO>                             │
└─────────────────────────────────────────────────────┘
                      ↕ ProcessBuilder
┌─────────────────────────────────────────────────────┐
│              Python Script                           │
│                                                      │
│  recommendation_engine.py                            │
│       │                                              │
│       │  mysql.connector.connect()                   │
│       ▼                                              │
│  Загрузка данных:                                    │
│    - artworks (одобренные)                           │
│    - likes                                           │
│    - comments                                        │
│    - categories                                      │
│       │                                              │
│       │  TfidfVectorizer + cosine_similarity         │
│       │  SimpleSVD (матричная факторизация)          │
│       ▼                                              │
│  hybrid_recommendations()                            │
│       │                                              │
│       ▼                                              │
│  JSON вывод:                                         │
│  {"recommendations": [...]}                          │
└─────────────────────────────────────────────────────┘
                      ↕ JDBC
┌─────────────────────────────────────────────────────┐
│                  MySQL (vag1)                        │
│                                                      │
│  Таблицы: artworks, users, likes,                   │
│           comments, categories,                      │
│           artwork_category                            │
└─────────────────────────────────────────────────────┘
```

---

## ✅ Текущее состояние

### Что работает:
- ✅ Python-скрипт подключается к MySQL
- ✅ Загружает реальные данные из базы
- ✅ Обучает SVD-модель
- ✅ Формирует гибридные рекомендации
- ✅ Возвращает JSON
- ✅ Java-код компилируется без ошибок
- ✅ Юнит-тесты проходят

### Что отключено:
- ❌ Spring-бины (закомментированы в XML)
- ❌ REST-контроллер (закомментированы аннотации)
- ❌ Модуль не загружается при старте приложения
- ❌ Не влияет на существующий функционал

---

## 🚀 Как активировать (кратко)

1. **Python зависимости:**
   ```bash
   pip install pandas numpy scikit-learn mysql-connector-python
   ```

2. **Настроить БД** в `recommendation_engine.py` (строки 27-28)

3. **Раскомментировать бины** в `recommendation-context.xml`

4. **Добавить import** в `dispatcher-servlet.xml`:
   ```xml
   <import resource="classpath:com/example/vag/recommendation/recommendation-context.xml" />
   ```

5. **Раскомментировать аннотации** в `RecommendationController.java`

6. **Включить модуль** в `recommendation.properties`:
   ```properties
   recommendation.enabled=true
   ```

**Полная инструкция:** `ML-Recommendation/INTEGRATION_GUIDE.md`

---

## 📊 Метрики качества

Python-скрипт автоматически рассчитывает:
- **Precision@K** — доля релевантных рекомендаций
- **Recall@K** — полнота охвата релевантных работ
- **Coverage** — разнообразие рекомендованных работ

Результаты выводятся при запуске в режиме демонстрации.

---

## 🔧 Настраиваемые параметры

### В `recommendation.properties`:
| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| `python.executable` | Путь к Python | `python` |
| `recommendation.script.path` | Путь к скрипту | `../ML-Recommendation/recommendation_engine.py` |
| `recommendation.default.topn` | Количество рекомендаций | `10` |
| `recommendation.alpha` | Вес коллаборативной фильтрации | `0.6` |
| `recommendation.enabled` | Включить модуль | `false` |

### В `recommendation_engine.py`:
| Параметр | Строка | Описание |
|----------|--------|----------|
| `n_factors` | 234 | Количество латентных факторов SVD |
| `lr` | 234 | Скорость обучения |
| `reg` | 234 | Регуляризация |
| `n_epochs` | 234 | Количество эпох обучения |
| `alpha` | 274 | Вес гибридизации |

---

## 📝 Пример использования (после активации)

### В Java-коде:
```java
@Autowired
private RecommendationService recommendationService;

public void showRecommendations(Long userId) {
    List<RecommendationDTO> recs = 
        recommendationService.getRecommendationsForUser(userId, 5);
    
    for (RecommendationDTO rec : recs) {
        System.out.println(rec.getTitle() + " — " + rec.getAuthor());
    }
}
```

### REST API:
```bash
GET /vag/api/recommendations?topN=5
```

Ответ:
```json
{
  "success": true,
  "recommendations": [
    {
      "artworkId": 5,
      "title": "Мона Лиза",
      "author": "Леонардо да Винчи",
      "categories": "портрет,ренессанс",
      "likes": 15,
      "score": 0.85
    }
  ],
  "count": 1
}
```

---

## ⚠️ Важно знать

1. **Производительность**: Первый запрос занимает 2-5 секунд (обучение модели). Рекомендуется кэширование.

2. **Thread-safe**: Каждый запрос создаёт новый процесс Python. Для production лучше запустить Python как отдельный микросервис.

3. **Безопасность**: Python-скрипт имеет доступ к MySQL с теми же правами, что и Java-приложение.

4. **Отказоустойчивость**: Если Python недоступен, сервис возвращает пустой список без ошибок.

---

## 📚 Документация

- `CODE_VAG/src/main/java/com/example/vag/recommendation/README.md` — документация модуля
- `ML-Recommendation/INTEGRATION_GUIDE.md` — пошаговая инструкция по активации
- `ML-Recommendation/Prototype RS.py` — оригинальный прототип из Colab
- `ML-Recommendation/recommendation_engine.py` — рабочий Python-скрипт

---

## 🎓 Алгоритмы

### Контентная фильтрация:
```
1. Для каждой работы создаётся текстовый признак: "Автор Категория1,Категория2"
2. TF-IDF векторизация всех признаков
3. Косинусное сходство между всеми парами работ
4. Для пользователя рекомендуются работы, похожие на лайкнутые
```

### Коллаборативная фильтрация (SVD):
```
1. Создаётся матрица пользователь-работа (рейтинги: лайк=1.0, комментарий=0.5)
2. Матричная факторизация через SGD:
   R ≈ U * V^T + biases
3. Для каждого пользователя предсказываются рейтинги всех работ
4. Рекомендуются работы с наивысшими предсказанными рейтингами
```

### Гибридный подход:
```
score = alpha * collab_score + (1 - alpha) * content_score
```
Где `alpha` настраивается (по умолчанию 0.6).
