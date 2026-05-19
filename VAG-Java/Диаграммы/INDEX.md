📊 **СОДЕРЖАНИЕ ДИАГРАММ VAG (Virtual Art Gallery)**

Этот документ служит навигатором по всем архитектурным диаграммам системы.

---

## 📑 Полный список диаграмм

| № | Файл | Тип | Уровень | Описание |
|---|------|-----|---------|---------|
| 1 | [C4_Context_Diagram.puml](#1-c4-context-diagram) | C4 Context | ⭐ Стартовый | Общий контекст системы с пользователями и внешними системами |
| 2 | [C4_Container_Diagram.puml](#2-c4-container-diagram) | C4 Container | ⭐⭐ Базовый | Архитектура контейнеров (приложения, БД, сервисы) |
| 3 | [C4_Component_Diagram.puml](#3-c4-component-diagram) | C4 Component | ⭐⭐⭐ Детальный | Структура Spring MVC приложения (контроллеры, сервисы, репозитории) |
| 4 | [C4_Deployment_Diagram.puml](#4-c4-deployment-diagram) | C4 Deployment | ⭐⭐⭐ Детальный | Развертывание на серверы и инфраструктура |
| 5 | [Sequence_Diagrams.puml](#5-sequence-diagrams) | UML Sequence | ⭐⭐⭐ Детальный | Четыре ключевых сценария с последовательностью вызовов |
| 6 | [Domain_Model_Diagram.puml](#6-domain-model-diagram) | Domain Model | ⭐⭐⭐ Детальный | Модель данных и связи между сущностями |

---

## 🎯 Когда использовать каждую диаграмму?

### 👔 Для менеджера/руководителя
**Читать в порядке:**
1. **Context Diagram** ← Поймет взаимодействие с пользователями
2. **Container Diagram** ← Увидит основные компоненты

**Цель:** Получить общее представление о системе и ее пользователях.

---

### 🏗️ Для архитектора
**Читать в порядке:**
1. **Context Diagram** ← Контекст системы
2. **Container Diagram** ← Архитектура на уровне контейнеров
3. **Component Diagram** ← Детальная структура приложения
4. **Deployment Diagram** ← Инфраструктура и развертывание
5. **Domain Model** ← Модель данных

**Цель:** Понять полную архитектуру системы и взаимодействие компонентов.

---

### 👨‍💻 Для разработчика
**Читать в порядке:**
1. **Component Diagram** ← Структура кода
2. **Domain Model** ← Сущности и их отношения
3. **Sequence Diagrams** ← Как компоненты взаимодействуют
4. **Container Diagram** ← Внешние зависимости
5. **Deployment Diagram** ← Как всё развернуто

**Цель:** Разобраться в коде и понять как работает система.

---

### 🧪 Для QA/тестировщика
**Читать в порядке:**
1. **Sequence Diagrams** ← Ключевые сценарии
2. **Component Diagram** ← Какие компоненты тестировать
3. **Domain Model** ← Какие данные использовать в тестах
4. **Container Diagram** ← Тестирование интеграций

**Цель:** Написать тест-кейсы на основе архитектуры.

---

## 📊 Подробное описание каждой диаграммы

### 1. C4 Context Diagram
**Файл:** `C4_Context_Diagram.puml`  
**Назначение:** Показать систему с точки зрения пользователя  
**Уровень абстракции:** Самый высокий  
**Читается за:** 2 минуты

#### Что показано:
```
┌─────────────────────────────────────────────────┐
│ Система VAG (Virtual Art Gallery)               │
│ ├─ Посетитель (Visitor)                         │
│ ├─ Художник (Artist)                            │
│ ├─ Администратор (Admin)                        │
│                                                  │
│ Зависит от:                                      │
│ ├─ MySQL Database                               │
│ ├─ MinIO File Storage                           │
│ └─ ML Recommendation Engine                     │
└─────────────────────────────────────────────────┘
```

#### Информация:
- ✅ 3 типа пользователей (Persons)
- ✅ Главная система VAG
- ✅ 3 внешние системы (External Systems)
- ✅ Все связи между ними (Relationships)

#### Используется для:
- 📌 Презентаций для менеджеров
- 📌 Документации проекта
- 📌 Обсуждения требований
- 📌 Поиска stakeholders

---

### 2. C4 Container Diagram
**Файл:** `C4_Container_Diagram.puml`  
**Назначение:** Показать основные контейнеры и их взаимодействие  
**Уровень абстракции:** Средний  
**Читается за:** 3-5 минут

#### Что показано:
```
┌─────────────────────────────────────────────────┐
│ Virtual Art Gallery System                       │
│ ├─ Web Browser (UI)                             │
│ ├─ Spring MVC Web Application (Backend)         │
│ ├─ MySQL Database                               │
│ ├─ MinIO File Storage                           │
│ └─ ML Recommendation Service (Python)           │
└─────────────────────────────────────────────────┘
```

#### Информация:
- ✅ 5 основных контейнеров
- ✅ Технологии для каждого контейнера
- ✅ Протоколы взаимодействия (HTTP, JDBC, S3 API)
- ✅ Описание каждого контейнера

#### Используется для:
- 📌 Архитектурных решений
- 📌 Выбора технологий
- 📌 Планирования развертывания
- 📌 Документации для команды

---

### 3. C4 Component Diagram
**Файл:** `C4_Component_Diagram.puml`  
**Назначение:** Показать внутреннюю структуру Spring MVC приложения  
**Уровень абстракции:** Детальный  
**Читается за:** 10-15 минут

#### Что показано:
```
Spring MVC Web Application
├─ Controllers Layer (9 компонентов)
│  ├─ AuthController
│  ├─ ArtworkController
│  ├─ CategoryController
│  ├─ ExhibitionController
│  ├─ AdminController
│  ├─ UserController
│  ├─ HomeController
│  ├─ UploadController
│  └─ RecommendationController
├─ Service Layer (8 компонентов)
│  ├─ UserService
│  ├─ ArtworkService
│  ├─ CategoryService
│  ├─ ExhibitionService
│  ├─ ModerationService
│  ├─ NotificationService
│  ├─ ImageFeatureService
│  └─ RecommendationService
├─ Data Access Layer (8 компонентов)
│  ├─ UserRepository
│  ├─ ArtworkRepository
│  ├─ CategoryRepository
│  ├─ ExhibitionRepository
│  ├─ CommentRepository
│  ├─ LikeRepository
│  ├─ NotificationRepository
│  └─ ImageHashRepository
├─ Model Layer (5 групп)
│  ├─ User, Role
│  ├─ Artwork, Category
│  ├─ Exhibition
│  ├─ Comment, Like, Notification
│  └─ ImageHash
└─ Integration Layer
   ├─ MinIO Client
   ├─ ML Engine Client
   └─ DTOs
```

#### Информация:
- ✅ 40+ компонентов Spring приложения
- ✅ Послойная архитектура (Controllers → Services → Repositories)
- ✅ Все связи между компонентами
- ✅ Интеграция с MinIO и ML Engine

#### Используется для:
- 📌 Разработки новых функций
- 📌 Код-ревью
- 📌 Обучения новых разработчиков
- 📌 Рефакторинга
- 📌 Поиска мест для оптимизации

---

### 4. C4 Deployment Diagram
**Файл:** `C4_Deployment_Diagram.puml`  
**Назначение:** Показать как компоненты развернуты в окружение  
**Уровень абстракции:** Детальный  
**Читается за:** 5-10 минут

#### Что показано:
```
┌─────────────────────────────────────────────────┐
│ User Device                                      │
│ └─ Web Browser                                  │
├─────────────────────────────────────────────────┤
│ Application Server (Linux)                      │
│ └─ Java Runtime (JDK 11)                       │
│    └─ Spring MVC Application (WAR)             │
├─────────────────────────────────────────────────┤
│ ML Server (Linux)                               │
│ └─ Python Runtime (3.8+)                       │
│    └─ ML Recommendation Engine                 │
├─────────────────────────────────────────────────┤
│ Storage Server (Linux)                          │
│ └─ Docker                                       │
│    └─ MinIO Container                          │
├─────────────────────────────────────────────────┤
│ Database Server (Linux/Windows)                 │
│ └─ Docker                                       │
│    └─ MySQL Container                          │
└─────────────────────────────────────────────────┘
```

#### Информация:
- ✅ 5 узлов развертывания
- ✅ Операционные системы для каждого узла
- ✅ Технологии и рантаймы
- ✅ Протоколы взаимодействия и порты

#### Используется для:
- 📌 DevOps конфигурации
- 📌 Docker/Kubernetes настроек
- 📌 Планирования инфраструктуры
- 📌 Масштабирования системы
- 📌 Документации для ops команды

---

### 5. Sequence Diagrams
**Файл:** `Sequence_Diagrams.puml`  
**Назначение:** Показать взаимодействие компонентов в ключевых сценариях  
**Уровень абстракции:** Детальный  
**Читается за:** 15-20 минут

#### Включены 4 сценария:

#### 🎯 Сценарий 1: Регистрация и вход Художника
```
Художник → Веб-браузер → AuthController → UserService → UserRepository → MySQL
             ↓ Валидация ↓ Хеширование пароля ↓ Сохранение в БД
```

**Что происходит:**
1. Художник регистрируется через форму
2. Данные валидируются на сервере
3. Пароль хешируется
4. Пользователь сохраняется в БД с ролью ARTIST
5. Художник может войти в систему

**Временная сложность:** O(1) для регистрации, O(n) для проверки уникальности email

---

#### 🎨 Сценарий 2: Загрузка произведения
```
Художник → Форма загрузки → UploadController → ArtworkService
    ↓
    MinIO Client → MinIO Storage (файл сохранен)
    ↓
    ImageFeatureService → ML Client → ML Engine (OpenCLIP анализирует)
    ↓
    ImageHashRepository → MySQL (признаки сохранены)
    ↓
    ArtworkRepository → MySQL (произведение сохранено)
    ↓
    NotificationService → Уведомляет администраторов
```

**Что происходит:**
1. Художник выбирает изображение и заполняет данные
2. Файл загружается в MinIO
3. Image Feature Service получает признаки от ML Engine
4. Признаки сохраняются в ImageHash
5. Произведение сохраняется в БД
6. Администраторы получают уведомление для модерирования

**Интеграции:**
- ✅ Spring MVC ↔ MinIO (S3 API)
- ✅ Spring MVC ↔ Python ML Engine (HTTP REST)
- ✅ Spring ↔ MySQL (JDBC + Hibernate)

---

#### 🔍 Сценарий 3: Просмотр и рекомендации
```
Посетитель → Веб-браузер → ArtworkController → MySQL (загружает произведение)
    ↓
    RecommendationController → RecommendationService
    ↓
    ML Client → ML Engine (ищет похожие)
    ↓
    ArtworkRepository → MySQL (загружает похожие произведения)
    ↓
    Веб-браузер → Отображает рекомендации
```

**Что происходит:**
1. Посетитель просматривает произведение
2. Параллельно загружаются рекомендации
3. ML Engine анализирует признаки и ищет похожие
4. Система загружает похожие произведения из БД
5. Отображаются рекомендации в UI

**Параллелизм:** Основная страница и рекомендации загружаются одновременно

---

#### ✅ Сценарий 4: Модерирование администратором
```
Администратор → Админ панель → AdminController → ModerationService
    ↓
    ArtworkRepository → MySQL (загружает произведение)
    ↓
    Устанавливает status = APPROVED
    ↓
    ArtworkRepository → MySQL (обновляет)
    ↓
    NotificationService → Уведомляет художника
    ↓
    Произведение опубликовано
```

**Что происходит:**
1. Администратор видит произведение на модерирование
2. Кликает "Одобрить"
3. Статус произведения меняется на APPROVED
4. Произведение становится видимым всем
5. Художник получает уведомление

**Альтернативные пути:**
- Администратор может отклонить произведение (status = REJECTED)
- С указанием причины отклонения

---

#### Информация:
- ✅ 4 ключевых сценария с полной последовательностью
- ✅ Все вызовы методов и их параметры
- ✅ Взаимодействие с БД, файловым хранилищем и ML
- ✅ Параллельные процессы (async операции)

#### Используется для:
- 📌 Понимания flow приложения
- 📌 Поиска узких мест (bottlenecks)
- 📌 Написания тест-кейсов
- 📌 Документации API
- 📌 Обучения новых разработчиков

---

### 6. Domain Model Diagram
**Файл:** `Domain_Model_Diagram.puml`  
**Назначение:** Показать модель данных и связи между сущностями  
**Уровень абстракции:** Детальный  
**Читается за:** 10-15 минут

#### Что показано:
```
User Domain
├─ User (id, email, password, name, bio, role)
└─ Role (VISITOR, ARTIST, ADMIN)

Artwork Domain
├─ Artwork (id, title, description, status, category)
├─ Category (id, name, description)
└─ ImageHash (features[], modelVersion, extractedAt)

Exhibition Domain
├─ Exhibition (id, title, description, artist)
└─ ExhibitionArtwork (relationship)

Interaction Domain
├─ Comment (id, text, author, artwork)
├─ Like (id, user, artwork)
└─ Notification (id, message, type, user)

Value Objects
├─ ArtworkStatus: PENDING, APPROVED, REJECTED, ARCHIVED
├─ NotificationType: ARTWORK_APPROVED, etc.
└─ RoleName: ROLE_VISITOR, ROLE_ARTIST, ROLE_ADMIN
```

#### Информация:
- ✅ 12 основных сущностей (Entities)
- ✅ 5 value objects
- ✅ Все связи и их типы (1:1, 1:N, N:M)
- ✅ Атрибуты каждой сущности
- ✅ Комментарии к сложным сущностям

#### Используется для:
- 📌 Проектирования БД
- 📌 Написания SQL запросов
- 📌 Entity моделей в Java
- 📌 Понимания бизнес-логики
- 📌 Миграций БД

---

## 🔄 Связи между диаграммами

```
Context Diagram
     ↓ (детализирует)
Container Diagram
     ↓ (детализирует)
Component Diagram
     ↓ (показывает flow)
Sequence Diagrams
     ↓ (операционализирует)
Domain Model Diagram
```

---

## 📋 Таблица компонентов

### Controllers (9 шт)
| Контроллер | Назначение |
|------------|-----------|
| AuthController | Вход/регистрация |
| HomeController | Главная страница |
| ArtworkController | Просмотр/редактирование произведений |
| CategoryController | Управление категориями |
| ExhibitionController | Создание/редактирование выставок |
| UserController | Профиль пользователя |
| AdminController | Админ-панель (модерирование) |
| UploadController | Загрузка файлов |
| RecommendationController | REST API рекомендаций |

### Services (8 шт)
| Сервис | Назначение |
|--------|-----------|
| UserService | Управление пользователями |
| ArtworkService | Бизнес-логика произведений |
| CategoryService | Управление категориями |
| ExhibitionService | Управление выставками |
| ModerationService | Модерирование контента |
| NotificationService | Уведомления |
| ImageFeatureService | Обработка признаков |
| RecommendationService | Рекомендации |

### Repositories (8 шт)
| Репозиторий | Назначение |
|------------|-----------|
| UserRepository | CRUD пользователей |
| ArtworkRepository | CRUD произведений |
| CategoryRepository | CRUD категорий |
| ExhibitionRepository | CRUD выставок |
| CommentRepository | CRUD комментариев |
| LikeRepository | CRUD лайков |
| NotificationRepository | CRUD уведомлений |
| ImageHashRepository | Хранение признаков |

---

## 🎯 Практические примеры использования

### Пример 1: Я новый разработчик, с чего начать?
```
1. Прочитайте Context Diagram (2 мин) → поймете что это такое
2. Посмотрите Container Diagram (5 мин) → поймете основные части
3. Изучите Component Diagram (15 мин) → увидите код
4. Прочитайте Domain Model (10 мин) → поймете БД
5. Посмотрите Sequence Diagram (20 мин) → как всё работает
6. Начните с простых задач в одной Service
```

### Пример 2: Я хочу добавить новую функцию
```
1. Посмотрите Component Diagram → найдите где должна быть функция
2. Посмотрите Domain Model → какие данные нужны
3. Посмотрите Sequence Diagrams → как похожие операции работают
4. Найдите нужный Controller → добавьте endpoint
5. Создайте Service → напишите логику
6. Создайте Repository → для работы с БД
```

### Пример 3: Я хочу оптимизировать производительность
```
1. Посмотрите Sequence Diagrams → найдите медленные операции
2. Посмотрите Component Diagram → найдите вызовы
3. Посмотрите Container Diagram → добавьте кеш? Async операции?
4. Проверьте запросы в БД → может быть нужен индекс?
```

---

## 🔧 Как экспортировать диаграммы

### Online (самый быстрый способ)
```
1. Откройте http://www.plantuml.com/plantuml/uml/
2. Вставьте код из .puml файла
3. Нажмите Export → PNG/SVG/PDF
```

### VS Code (удобно для работы)
```
1. Установите расширение PlantUML (jebbs.plantuml)
2. Откройте .puml файл
3. Alt+D → открыть предпросмотр
4. Ctrl+Shift+P → Export
```

### Командная строка
```bash
plantuml -tpng *.puml          # PNG
plantuml -tsvg *.puml          # SVG
plantuml -tpdf *.puml          # PDF
```

---

## 📚 Дополнительные ресурсы

- **C4 Model:** https://c4model.com/
- **PlantUML:** https://plantuml.com/
- **Spring Framework:** https://spring.io/projects/spring-framework
- **MinIO Docs:** https://docs.min.io/
- **OpenCLIP:** https://github.com/OpenAI/CLIP

---

## ✅ Чек-лист для понимания архитектуры

- [ ] Я понимаю 3 роли пользователей
- [ ] Я знаю основные 5 контейнеров
- [ ] Я знаю названия 9 контроллеров
- [ ] Я знаю 8 сервисов
- [ ] Я знаю как загружается произведение
- [ ] Я знаю как работают рекомендации
- [ ] Я знаю как модерируется контент
- [ ] Я понимаю модель данных (12 сущностей)
- [ ] Я знаю, почему используется MinIO
- [ ] Я знаю, как развертывается система

---

## 🔗 Быстрая навигация

| Нужно | Смотрите | За сколько минут |
|------|----------|-----------------|
| Понять, что это | Context Diagram | 2 мин |
| Увидеть общую архитектуру | Container Diagram | 5 мин |
| Разобраться в коде | Component Diagram | 15 мин |
| Понять процессы | Sequence Diagrams | 20 мин |
| Понять БД | Domain Model | 10 мин |
| Развернуть систему | Deployment Diagram | 10 мин |

**Итого на полное изучение:** ~60 минут

---

**Версия:** 1.0  
**Дата:** 19 мая 2026 г.  
**Статус:** ✅ Полная информация
