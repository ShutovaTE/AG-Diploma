# Модуль рекомендаций для Virtual Art Gallery

Гибридная система рекомендаций на основе контентной и коллаборативной фильтрации.

## 📁 Структура модуля

```
recommendation/
├── dto/
│   └── RecommendationDTO.java          # DTO для передачи данных рекомендаций
├── service/
│   ├── RecommendationService.java      # Интерфейс сервиса
│   └── RecommendationServiceImpl.java  # Реализация (вызов Python-скрипта)
├── controller/
│   └── RecommendationController.java   # REST API (закомментирован)
├── recommendation-context.xml          # Spring конфигурация (закомментирована)
├── recommendation.properties           # Настраиваемые параметры
└── README.md                           # Этот файл
```

## 🚀 Как активировать

### Шаг 1: Подготовка Python-окружения

1. Убедитесь, что Python 3.8+ установлен:
   ```bash
   python --version
   ```

2. Установите необходимые библиотеки:
   ```bash
   pip install pandas numpy scikit-learn mysql-connector-python
   ```

3. Настройте подключение к БД в файле `ML-Recommendation/recommendation_engine.py`:
   ```python
   # Строки 27-28
   user='root',      # Ваш пользователь MySQL
   password='root'   # Ваш пароль MySQL
   ```

### Шаг 2: Активация в Spring

1. **Раскомментируйте bean-определения** в `recommendation-context.xml`:
   ```xml
   <bean id="recommendationService" 
         class="com.example.vag.recommendation.service.RecommendationServiceImpl" />
   
   <bean id="recommendationController" 
         class="com.example.vag.recommendation.controller.RecommendationController">
       <constructor-arg ref="recommendationService" />
   </bean>
   ```

2. **Добавьте component-scan** в `dispatcher-servlet.xml` (если ещё не добавлен):
   ```xml
   <context:component-scan base-package="com.example.vag.recommendation" />
   ```

3. **Импортируйте конфигурацию** в `dispatcher-servlet.xml`:
   ```xml
   <import resource="classpath:com/example/vag/recommendation/recommendation-context.xml" />
   ```

4. **Раскомментируйте аннотации** в `RecommendationController.java`:
   - Уберите `//` с `@RestController`, `@RequestMapping`, `@GetMapping` и т.д.
   - Следуйте инструкциям в комментариях контроллера

5. **Включите модуль** в `recommendation.properties`:
   ```properties
   recommendation.enabled=true
   ```

### Шаг 3: Проверка работоспособности

Запустите приложение и проверьте статус системы:

```bash
GET http://localhost:8080/vag/api/recommendations/status
```

Ожидаемый ответ:
```json
{
  "available": true,
  "message": "Система рекомендаций готова к работе"
}
```

## 🔌 Использование

### Получение рекомендаций

**Для авторизованного пользователя:**
```bash
GET http://localhost:8080/vag/api/recommendations?topN=10
Authorization: Bearer <token>
```

**Для конкретного пользователя (только админ):**
```bash
GET http://localhost:8080/vag/api/recommendations/1?topN=5
Authorization: Bearer <admin-token>
```

**Пример ответа:**
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
    },
    {
      "artworkId": 12,
      "title": "Звёздная ночь",
      "author": "Винсент Ван Гог",
      "categories": "пейзаж,постимпрессионизм",
      "likes": 23,
      "score": 0.78
    }
  ],
  "count": 2
}
```

### Использование в других сервисах

Вы можете внедрить `RecommendationService` в любой существующий сервис:

```java
// В ArtworkController.java или HomeController.java
private final RecommendationService recommendationService;

// В конструкторе:
public ArtworkController(..., RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
}

// В методе контроллера:
List<RecommendationDTO> recommendations = 
    recommendationService.getRecommendationsForUser(userId, 5);
model.addAttribute("recommendations", recommendations);
```

## ⚙️ Настройка

### Параметры в `recommendation.properties`

| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| `python.executable` | Путь к Python | `python` |
| `recommendation.script.path` | Путь к скрипту | `../ML-Recommendation/recommendation_engine.py` |
| `recommendation.default.topn` | Количество рекомендаций | `10` |
| `recommendation.alpha` | Вес коллаборативной фильтрации (0.0-1.0) | `0.6` |
| `recommendation.enabled` | Включить модуль | `false` |

### Настройка Python-скрипта

В `recommendation_engine.py` можно изменить:

- **Гиперпараметры SVD** (строка 234):
  ```python
  svd = SimpleSVD(n_factors=10, lr=0.005, reg=0.02, n_epochs=20)
  ```

- **Вес гибридизации** (вызов `hybrid_recommendations`):
  ```python
  alpha=0.6  # 60% коллаборативная, 40% контентная
  ```

## 🔍 Алгоритм работы

1. Java вызывает Python-скрипт через `ProcessBuilder`
2. Python подключается к MySQL, загружает данные
3. Строит контентные признаки (TF-IDF + косинусное сходство)
4. Обучает SVD-модель на лайках/комментариях
5. Комбинирует оба подхода с весом `alpha`
6. Возвращает JSON с рекомендациями

## ⚠️ Важно

- **Модуль отключён по умолчанию** — приложение работает без него
- **Нет жёсткой зависимости** от Python — если скрипт недоступен, возвращается пустой список
- **Thread-safe** — каждый запрос создаёт новый процесс Python
- **Логирование** — все ошибки пишутся в Java Logger

## 🐛 Troubleshooting

| Проблема | Решение |
|----------|---------|
| `Python недоступен` | Проверьте, что Python в PATH или укажите полный путь в `recommendation.properties` |
| `Скрипт не найден` | Проверьте путь в `recommendation.properties` |
| `Ошибка подключения к БД` | Настройте параметры подключения в `recommendation_engine.py` (строки 27-28) |
| `Нет данных взаимодействий` | Добавьте лайки/комментарии в базу данных |
| `ModuleNotFoundError` | Установите зависимости: `pip install pandas numpy scikit-learn mysql-connector-python` |

## 📊 Метрики качества

Модуль автоматически рассчитывает:
- **Precision@K** — точность рекомендаций
- **Recall@K** — полнота рекомендаций
- **Coverage** — охват каталога

Результаты выводятся в лог при запуске Python-скрипта в режиме демонстрации.
