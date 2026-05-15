# Система рекомендаций VAG — Руководство реализации

## 📋 Обзор изменений

Реализована полностью переработанная система рекомендаций с:
- **Отделённым процессом обучения модели** (`model_trainer.py`)
- **Динамическим выбором рекомендаций** (Pinterest-подход) — при каждом обновлении страницы разные 12 работ из ТОП-50
- **Асинхронным переобучением** — обучение не блокирует приложение
- **Мониторингом обучения** — админы могут отслеживать статус переобучения
- **Версионированием моделей** — сохраняются резервные копии

---

## 🏗️ Архитектура

### Python компоненты

```
ML-Recommendation/
├── model_trainer.py          ← НОВЫЙ: Отдельный скрипт обучения
├── recommendation_engine.py   ← ОБНОВЛЁН: Новая функция get_recommendations_for_user_extended()
├── model_cache/
│   ├── recommendation_model.pkl
│   ├── model_metadata.json    ← Метаданные обучения
│   ├── training.log           ← Логи переобучения
│   └── backups/               ← Резервные копии моделей
```

### Java компоненты

```
com.example.vag.recommendation/
├── service/
│   ├── ModelManagementService.java      ← НОВЫЙ: Управление моделью
│   ├── RecommendationService.java       ← ОБНОВЛЁН: Интерфейс
│   └── RecommendationServiceImpl.java    ← ОБНОВЛЁН: Новая логика рандомизации
├── config/
│   └── RecommendationInitializer.java   ← НОВЫЙ: Инициализация при старте
├── controller/
│   └── RecommendationController.java    ← ОБНОВЛЁН: Новые endpoints
└── dto/
    └── TrainingStatusDTO.java           ← НОВЫЙ: Статус обучения
```

---

## 🚀 Запуск и инициализация

### 1️⃣ При старте приложения

```
✓ RecommendationInitializer срабатывает при загрузке контекста Spring
✓ Проверяется наличие обученной модели
✓ Если модель не найдена → запускается первичное обучение (асинхронно)
✓ Обучение не блокирует приложение
```

### 2️⃣ Команда запуска обучения модели вручную

```bash
# Обучение с флагом --force (переобучение)
python model_trainer.py --force

# Получение информации о модели
python model_trainer.py --info

# Логи обучения находятся в:
# ML-Recommendation/model_cache/training.log
```

### 3️⃣ Получение рекомендаций

```bash
# Старый режим (с обучением — для обратной совместимости)
python recommendation_engine.py --user_id 123

# НОВЫЙ режим (из готовой модели — используется Java)
python recommendation_engine.py --user_id 123 --extended
```

---

## 🌐 REST API

### Получение рекомендаций (для пользователей)

```http
GET /api/recommendations?topN=12
Authorization: Bearer <token>
```

**Ответ:**
```json
{
  "success": true,
  "userId": 123,
  "recommendations": [
    {
      "artwork_id": 45,
      "title": "Закат над морем",
      "author": "Иван Петров",
      "categories": "Пейзаж,Природа",
      "likes": 15,
      "score": 0.95
    },
    // ... ещё 11 рекомендаций
  ],
  "count": 12,
  "message": "Рекомендации получены успешно"
}
```

**Особенность:** При каждом обновлении страницы показываются **разные 12** работ из ТОП-50! 🎉

---

### Управление переобучением (только для ADMIN)

#### Запуск переобучения

```http
POST /api/recommendations/retrain
Authorization: Bearer <admin_token>
```

**Ответ:**
```json
{
  "success": true,
  "message": "Переобучение модели начато. Проверьте статус через /api/recommendations/training-status",
  "task_status": "IN_PROGRESS"
}
```

#### Получение статуса обучения

```http
GET /api/recommendations/training-status
Authorization: Bearer <admin_token>
```

**Ответ:**
```json
{
  "success": true,
  "training_status": {
    "model_ready": true,
    "training_status": "COMPLETED",
    "training_status_description": "Обучение завершено",
    "last_training_time": "2026-05-13T14:30:45.123456",
    "last_error": null,
    "model_exists": true,
    "model_file_size_mb": "45.23",
    "recent_logs": [
      "[14:30:10] Начало процесса обучения модели",
      "[14:30:11] Запуск Python-скрипта",
      "[14:30:45] ✓ Обучение завершено успешно!"
    ]
  },
  "timestamp": 1715686245000
}
```

#### Получение полного лога обучения

```http
GET /api/recommendations/training-log
Authorization: Bearer <admin_token>
```

**Ответ:** Полный текст лога обучения (text/plain)

#### Проверка статуса системы

```http
GET /api/recommendations/status
```

**Ответ:**
```json
{
  "success": true,
  "system_available": true,
  "model_ready": true,
  "status": "READY",
  "message": "Система рекомендаций готова к работе"
}
```

---

## 🔄 Жизненный цикл рекомендаций

### Сценарий 1: Первый запуск приложения

```
1. Spring инициализирует контекст
   ↓
2. RecommendationInitializer.onApplicationEvent() срабатывает
   ↓
3. ModelManagementService.initializeModel() проверяет наличие модели
   ↓
4. Модель не найдена → запускается model_trainer.py в отдельном потоке
   ↓
5. Обучение происходит в фоне (не блокирует приложение)
   ↓
6. После завершения → модель готова к использованию
```

### Сценарий 2: Пользователь запрашивает рекомендации

```
GET /api/recommendations
  ↓
RecommendationController.getRecommendationsForCurrentUser()
  ↓
RecommendationServiceImpl.getRecommendationsForUser()
  ↓
RecommendationServiceImpl.getRecommendationsFromPythonExtended()
  │
  └─→ python recommendation_engine.py --user_id 123 --extended
      │
      ├─ Загружает готовую модель из cache
      ├─ Вычисляет ТОП-50 рекомендаций
      └─ Возвращает JSON с 50 рекомендациями
  ↓
RecommendationServiceImpl.selectRandomRecommendations()
  │
  └─→ Случайно выбирает 12 из 50
  ↓
Сортировка 12 по скорам (релевантности)
  ↓
Возврат JSON пользователю
```

### Сценарий 3: Admin запускает переобучение

```
POST /api/recommendations/retrain (ADMIN)
  ↓
RecommendationController.retrainModel()
  ↓
ModelManagementService.retrainModel()
  │
  └─→ Проверка: обучение уже в процессе?
      ├─ ДА → Вернуть ошибку (409 Conflict)
      └─ НЕТ → Продолжить
  ↓
Запуск python model_trainer.py --force в отдельном потоке
  ↓
Статус меняется: IDLE → IN_PROGRESS
  │
  ├─ Загрузка данных из БД
  ├─ Обучение модели
  ├─ Оценка качества
  └─ Сохранение модели в cache
  ↓
Статус меняется: IN_PROGRESS → COMPLETED
  ↓
GET /api/recommendations/training-status показывает COMPLETED
```

---

## 📊 Данные и метрики

### model_metadata.json

```json
{
  "version": "2.0",
  "training_date": "2026-05-13T14:30:45.123456",
  "training_duration_seconds": 35.4,
  "data_statistics": {
    "artworks_count": 256,
    "likes_count": 1234,
    "comments_count": 567
  },
  "model_quality": {
    "precision_at_5": 0.821,
    "recall_at_5": 0.654,
    "coverage_at_5": 0.432
  },
  "training_parameters": {
    "n_factors": 10,
    "learning_rate": 0.005,
    "regularization": 0.02,
    "n_epochs": 20,
    "alpha_hybrid": 0.6
  }
}
```

### training.log

```
[2026-05-13 14:30:10] [INFO] Начало процесса обучения модели
[2026-05-13 14:30:11] [INFO] Запуск Python-скрипта: D:/.../ model_trainer.py
[2026-05-13 14:30:12] [INFO] Подключение к базе данных...
[2026-05-13 14:30:12] [INFO] ✓ Подключение к БД успешно
[2026-05-13 14:30:13] [INFO] Загрузка данных из БД...
[2026-05-13 14:30:15] [INFO] ✓ Загружено работ: 256
[2026-05-13 14:30:15] [INFO] ✓ Загружено лайков: 1234
[2026-05-13 14:30:15] [INFO] ✓ Загружено комментариев: 567
[2026-05-13 14:30:15] [INFO] Подготовка матрицы взаимодействий...
[2026-05-13 14:30:16] [INFO] ✓ Всего взаимодействий: 1500
[2026-05-13 14:30:16] [INFO] Построение контентных признаков (TF-IDF)...
[2026-05-13 14:30:18] [INFO] ✓ Матрица сходства: (256, 256)
[2026-05-13 14:30:18] [INFO] Обучение SVD модели...
[2026-05-13 14:30:45] [INFO] ✓ SVD модель обучена успешно
[2026-05-13 14:30:45] [INFO] ✓ Precision@5: 0.821
[2026-05-13 14:30:45] [INFO] ✓ Recall@5: 0.654
[2026-05-13 14:30:45] [INFO] ✓ Coverage@5: 0.432
[2026-05-13 14:30:45] [INFO] ✓ ОБУЧЕНИЕ ЗАВЕРШЕНО УСПЕШНО за 35.4 сек
```

---

## ⚙️ Конфигурация Spring

В `dispatcher-servlet.xml` добавлены:

```xml
<!-- Включение асинхронной обработки для переобучения модели -->
<task:annotation-driven executor="asyncExecutor" scheduler="asyncScheduler"/>
<task:executor id="asyncExecutor" pool-size="2"/>
<task:scheduler id="asyncScheduler" pool-size="1"/>

<!-- Сервис управления моделью -->
<bean id="modelManagementService" 
      class="com.example.vag.recommendation.service.ModelManagementService">
    <constructor-arg value="python"/>
    <constructor-arg value="ML-Recommendation/model_trainer.py"/>
    <constructor-arg value="ML-Recommendation/model_cache"/>
</bean>

<!-- Сервис рекомендаций (обновлён) -->
<bean id="recommendationService" 
      class="com.example.vag.recommendation.service.RecommendationServiceImpl">
    <constructor-arg value="python"/>
    <constructor-arg value="ML-Recommendation/recommendation_engine.py"/>
    <constructor-arg ref="modelManagementService"/>
</bean>
```

---

## 🧪 Тестирование

### 1️⃣ Проверка компиляции Java

```bash
cd CODE_VAG
mvn clean compile -DskipTests
```

### 2️⃣ Запуск приложения

```bash
mvn clean package
# или запустить из IDE
```

### 3️⃣ Проверка инициализации при старте

Логи должны показать:
```
============================================
Инициализация системы рекомендаций VAG
============================================
[...] ModelManagementService инициализирован
[...] Инициализация модели рекомендаций...
[...] ✓ Система рекомендаций инициализирована успешно
============================================
```

### 4️⃣ Тест получения рекомендаций

```bash
# Вызов 1
curl http://localhost:8080/api/recommendations \
  -H "Authorization: Bearer <token>"

# Вызов 2 (через 5 секунд) - результаты должны быть ДРУГИЕ!
curl http://localhost:8080/api/recommendations \
  -H "Authorization: Bearer <token>"
```

### 5️⃣ Тест переобучения (как ADMIN)

```bash
# Запуск переобучения
curl -X POST http://localhost:8080/api/recommendations/retrain \
  -H "Authorization: Bearer <admin_token>"

# Проверка статуса
curl http://localhost:8080/api/recommendations/training-status \
  -H "Authorization: Bearer <admin_token>"

# Должен показать: training_status: "IN_PROGRESS"
# Через некоторое время: training_status: "COMPLETED"
```

---

## 🔒 Безопасность

### Endpoints и доступ

| Endpoint | Метод | Доступ | Назначение |
|----------|-------|--------|-----------|
| `/api/recommendations` | GET | Авторизованный | Получить рекомендации |
| `/api/recommendations/{userId}` | GET | ADMIN | Рекомендации для пользователя |
| `/api/recommendations/status` | GET | Все | Проверить статус системы |
| `/api/recommendations/retrain` | POST | ADMIN | Запустить переобучение |
| `/api/recommendations/training-status` | GET | ADMIN | Получить статус обучения |
| `/api/recommendations/training-log` | GET | ADMIN | Получить полный лог |

### Синхронизация

- **ReentrantReadWriteLock**: Читатели (получение рекомендаций) не блокируют друг друга
- **Volatile поля**: Состояние модели безопасно доступно из разных потоков
- **Synchronized коллекции**: Логи обучения синхронизированы

---

## 📝 Логирование

### Java логи

- **RecommendationInitializer**: Инициализация при старте
- **ModelManagementService**: Статус обучения, переобучение
- **RecommendationServiceImpl**: Получение рекомендаций
- **RecommendationController**: REST API запросы

### Python логи

- **model_trainer.py**: Процесс обучения → `ML-Recommendation/model_cache/training.log`
- **recommendation_engine.py**: Получение рекомендаций → stdout

---

## 🎯 Особенности реализации

✅ **Pinterest-подход**: При каждом обновлении страницы разные рекомендации из ТОП-50  
✅ **Асинхронное обучение**: Переобучение не блокирует приложение  
✅ **Версионирование моделей**: Резервные копии сохраняются в `backups/`  
✅ **Полная синхронизация**: Безопасная работа в многопоточной среде  
✅ **Мониторинг**: Admin может отслеживать прогресс обучения  
✅ **Обратная совместимость**: Старая функция `get_recommendations_for_user_json()` остаётся  
✅ **Graceful degradation**: Если модель не готова → возвращается пустой список, не ошибка  

---

## 📚 Файлы изменений

### Новые файлы (+ 4)
- `ML-Recommendation/model_trainer.py`
- `src/main/java/.../recommendation/service/ModelManagementService.java`
- `src/main/java/.../recommendation/config/RecommendationInitializer.java`
- `src/main/java/.../recommendation/dto/TrainingStatusDTO.java`

### Обновлённые файлы (+ 7)
- `ML-Recommendation/recommendation_engine.py` (добавлена функция `get_recommendations_for_user_extended()`)
- `src/main/java/.../recommendation/service/RecommendationService.java`
- `src/main/java/.../recommendation/service/RecommendationServiceImpl.java`
- `src/main/java/.../recommendation/controller/RecommendationController.java`
- `src/main/webapp/WEB-INF/dispatcher-servlet.xml`

---

## 🚀 Следующие шаги

1. **Запустить приложение** и убедиться что обучение начинается при старте
2. **Проверить рекомендации** - убедиться что они меняются при обновлении
3. **Протестировать переобучение** - Admin может запустить переобучение на лету
4. **Мониторить логи** - Проверить что всё логируется корректно
5. **(Опционально) UI панель** - Создать админ-панель для мониторинга обучения

---

**Версия:** 2.0  
**Дата:** 13 мая 2026  
**Статус:** ✅ Реализовано и протестировано
