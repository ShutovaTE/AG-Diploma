# Инструкция по активации модуля рекомендаций

## Текущее состояние

✅ Модуль рекомендаций **полностью отключён** и **не влияет** на работу приложения.

Приложение запускается и работает как обычно.

---

## Пошаговая активация

### Этап 1: Подготовка Python-окружения

#### 1.1. Проверка Python
```bash
python --version
```
Должна быть версия 3.8+.

#### 1.2. Установка зависимостей
```bash
pip install pandas numpy scikit-learn mysql-connector-python
```

#### 1.3. Настройка подключения к БД

Откройте файл: `d:\Git\AG-Diploma\VAG-Java\ML-Recommendation\recommendation_engine.py`

Найдите строки **27-28** и замените на ваши данные:
```python
connection = mysql.connector.connect(
    host='localhost',
    port=3306,
    database='vag1',
    user='ВАШ_ПОЛЬЗОВАТЕЛЬ',      # Например: 'root'
    password='ВАШ_ПАРОЛЬ'          # Ваш пароль от MySQL
)
```

#### 1.4. Тестовый запуск Python-скрипта
```bash
python "d:\Git\AG-Diploma\VAG-Java\ML-Recommendation\recommendation_engine.py"
```

Ожидаемый вывод:
```
============================================================
  Рекомендательная система Virtual Art Gallery
============================================================

Загрузка данных из базы...
  Загружено работ: X
  Загружено лайков: Y
  ...
```

---

### Этап 2: Активация в Spring

#### 2.1. Раскомментировать бины

Откройте: `CODE_VAG/src/main/java/com/example/vag/recommendation/recommendation-context.xml`

Замените закомментированные бины на:
```xml
<!-- Сервис рекомендаций -->
<bean id="recommendationService" 
      class="com.example.vag.recommendation.service.RecommendationServiceImpl" />

<!-- REST-контроллер рекомендаций -->
<bean id="recommendationController" 
      class="com.example.vag.recommendation.controller.RecommendationController">
    <constructor-arg ref="recommendationService" />
</bean>
```

#### 2.2. Добавить import в dispatcher-servlet.xml

Откройте: `CODE_VAG/src/main/webapp/WEB-INF/dispatcher-servlet.xml`

Добавьте в конец файла (перед `</beans>`):
```xml
<!-- Модуль рекомендаций -->
<import resource="classpath:com/example/vag/recommendation/recommendation-context.xml" />
```

#### 2.3. Включить Jackson для JSON

Проверьте, что в `pom.xml` есть зависимость Jackson (должна быть для Spring MVC):
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.13.3</version>
</dependency>
```

Если нет — добавьте в `pom.xml`.

#### 2.4. Раскомментировать аннотации в контроллере

Откройте: `RecommendationController.java`

Замените:
```java
// @RestController
// @RequestMapping("/api/recommendations")
public class RecommendationController {
```

На:
```java
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
```

Раскомментируйте все методы с `@GetMapping` и `@PreAuthorize`.

#### 2.5. Настроить параметр topN

Откройте: `recommendation.properties`

Измените:
```properties
recommendation.enabled=true
```

---

### Этап 3: Интеграция с существующими контроллерами (опционально)

Если хотите отображать рекомендации на страницах Thymeleaf:

#### 3.1. Добавить сервис в HomeController

Откройте: `HomeController.java`

Добавьте в конструктор:
```java
private final RecommendationService recommendationService;

public HomeController(..., RecommendationService recommendationService) {
    // ... существующие зависимости
    this.recommendationService = recommendationService;
}
```

#### 3.2. Добавить рекомендации в модель

В методе `home()` добавьте:
```java
// Получение рекомендаций для авторизованного пользователя
if (authentication != null && authentication.isAuthenticated()) {
    // Получение пользователя из БД
    String username = authentication.getName();
    User user = userService.findByUsername(username);
    
    if (user != null) {
        List<RecommendationDTO> recommendations = 
            recommendationService.getRecommendationsForUser(user.getId(), 5);
        model.addAttribute("recommendations", recommendations);
    }
}
```

#### 3.3. Отображение в Thymeleaf

Создайте файл: `CODE_VAG/src/main/webapp/WEB-INF/views/fragments/recommendations.html`

```html
<div th:if="${recommendations != null and !recommendations.isEmpty()}" class="recommendations-section">
    <h2>Рекомендуем для вас</h2>
    <div class="recommendations-grid">
        <div th:each="rec : ${recommendations}" class="recommendation-card">
            <a th:href="@{/artwork/details/{id}(id=${rec.artworkId})}">
                <h3 th:text="${rec.title}">Название работы</h3>
                <p th:text="${rec.author}">Автор</p>
                <p th:text="${rec.categories}">Категории</p>
                <span class="score" th:text="'Совпадение: ' + ${#numbers.formatPercent(rec.score, 0, 0)}">85%</span>
            </a>
        </div>
    </div>
</div>
```

---

### Этап 4: Проверка

#### 4.1. Запуск приложения
```bash
mvn clean package
```

Разверните WAR на Tomcat.

#### 4.2. Проверка REST API

```bash
# Статус системы
curl http://localhost:8080/vag/api/recommendations/status

# Ожидается:
# {"available": true, "message": "Система рекомендаций готова к работе"}
```

#### 4.3. Проверка рекомендаций

Авторизуйтесь и перейдите на главную страницу. Должен появиться блок "Рекомендуем для вас".

---

## Отключение модуля

Если нужно отключить модуль:

1. В `recommendation.properties`:
   ```properties
   recommendation.enabled=false
   ```

2. Или закомментируйте `<import>` в `dispatcher-servlet.xml`

3. Перезапустите приложение

---

## Troubleshooting

| Ошибка | Решение |
|--------|---------|
| `Python недоступен` | Добавьте Python в PATH или укажите полный путь в `recommendation.properties` |
| `Ошибка подключения к БД` | Проверьте параметры подключения в `recommendation_engine.py` |
| `ModuleNotFoundError: No module named 'pandas'` | `pip install pandas numpy scikit-learn mysql-connector-python` |
| `JSON parse error` | Запустите скрипт вручную и проверите вывод на корректность JSON |
| `Bean recommendationService not found` | Раскомментируйте бин в `recommendation-context.xml` |

---

## Архитектура решения

```
┌─────────────────────┐
│   Thymeleaf View    │  ← Отображение рекомендаций на странице
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  HomeController     │  ← Добавляет рекомендации в Model
└──────────┬──────────┘
           │
┌──────────▼──────────────────┐
│  RecommendationService      │  ← Java-интерфейс
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│  RecommendationServiceImpl  │  ← Вызов Python через ProcessBuilder
└──────────┬──────────────────┘
           │
           │  python recommendation_engine.py --user_id 1
           ▼
┌──────────────────────────────┐
│  recommendation_engine.py    │  ← Подключение к MySQL, ML-модель
│  (pandas, numpy, sklearn)    │  ← Возврат JSON
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│       MySQL (vag1)           │  ← artworks, likes, comments, categories
└──────────────────────────────┘
```

---

## Что происходит при запросе

1. Пользователь заходит на главную страницу
2. `HomeController` вызывает `recommendationService.getRecommendationsForUser(userId, 5)`
3. Java запускает: `python recommendation_engine.py --user_id 1`
4. Python подключается к MySQL, загружает данные
5. Строит ML-модель, формирует рекомендации
6. Возвращает JSON: `{"recommendations": [...]}`
7. Java парсит JSON, возвращает `List<RecommendationDTO>`
8. Controller добавляет в Model, Thymeleaf отображает

---

## Производительность

- **Первый запрос**: ~2-5 секунд (обучение модели)
- **Последующие запросы**: ~1-2 секунды
- **Кэширование**: Рекомендуется добавить кэширование (`@Cacheable`) для production

Для production можно:
- Запускать Python-скрипт как отдельный микросервис
- Кэшировать рекомендации на 1 час
- Обучать модель раз в сутки по расписанию
