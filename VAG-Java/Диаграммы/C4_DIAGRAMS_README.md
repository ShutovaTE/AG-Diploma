# C4 Диаграммы Virtual Art Gallery (VAG)

Этот каталог содержит архитектурные диаграммы системы Virtual Art Gallery в нотации **C4 Model** на базе PlantUML.

## 📋 Описание диаграмм

### 1. **C4_Context_Diagram.puml** - Диаграмма контекста
Показывает систему с точки зрения пользователя и ее взаимодействие с внешними системами.

**Элементы:**
- **Пользователи (Persons):**
  - 👥 Посетитель - незарегистрированный пользователь, может просматривать выставки и произведения
  - 🎨 Художник - авторизованный пользователь, может загружать произведения и создавать выставки
  - 👨‍💼 Администратор - администратор и модератор в одном лице, управляет контентом и пользователями

- **Основная система:**
  - 🖼️ VAG (Virtual Art Gallery) - платформа для создания и просмотра виртуальных выставок

- **Внешние системы:**
  - 🗄️ MySQL Database - хранилище данных
  - 📦 MinIO File Storage - объектное хранилище для изображений
  - 🤖 ML Recommendation Engine - система рекомендаций на базе OpenCLIP

---

### 2. **C4_Container_Diagram.puml** - Диаграмма контейнеров
Показывает основные контейнеры (приложения, базы данных, сервисы) внутри системы и их взаимодействие.

**Контейнеры:**
- **Web Browser** (HTML5/CSS/JavaScript/Thymeleaf)
  - Интерфейс пользователя для всех ролей
  - Просмотр галереи, управление контентом, администрирование

- **Spring MVC Web Application** (Spring 5.3.20, Java 11)
  - Основное приложение с REST API
  - Обработка бизнес-логики
  - Интеграция со всеми компонентами

- **MySQL Database** (MySQL 8.0.33)
  - Персистентное хранилище данных
  - Таблицы: пользователи, произведения, выставки, комментарии, лайки, уведомления

- **MinIO File Storage** (S3-compatible)
  - Хранилище файлов изображений
  - REST API для загрузки/скачивания

- **ML Recommendation Service** (Python, OpenCLIP, FastAPI)
  - Система рекомендаций
  - Анализ признаков изображений
  - REST API для получения рекомендаций

---

### 3. **C4_Component_Diagram.puml** - Диаграмма компонентов
Показывает подробную структуру Spring MVC приложения с всеми компонентами.

**Слои приложения:**

#### **Web Layer (Controllers)**
- `AuthController` - аутентификация и авторизация
- `HomeController` - домашняя страница
- `ArtworkController` - управление произведениями
- `CategoryController` - управление категориями
- `ExhibitionController` - управление выставками
- `UserController` - профиль пользователя
- `AdminController` - административные функции
- `UploadController` - загрузка файлов
- `RecommendationController` - REST API рекомендаций

#### **Service Layer (Business Logic)**
- `UserService` - управление пользователями и ролями
- `ArtworkService` - бизнес-логика произведений
- `CategoryService` - управление категориями
- `ExhibitionService` - управление выставками
- `ModerationService` - модерирование контента
- `NotificationService` - отправка уведомлений
- `ImageFeatureService` - обработка признаков изображений
- `RecommendationService` - получение рекомендаций от ML-движка
- `CustomUserDetailsService` - Spring Security аутентификация

#### **Data Access Layer (Repositories)**
- `UserRepository` - доступ к данным пользователей
- `ArtworkRepository` - доступ к данным произведений
- `CategoryRepository` - доступ к данным категорий
- `ExhibitionRepository` - доступ к данным выставок
- `CommentRepository` - доступ к комментариям
- `LikeRepository` - доступ к лайкам
- `NotificationRepository` - доступ к уведомлениям
- `ImageHashRepository` - доступ к хешам изображений

#### **Model Layer (Entities)**
- `User`, `Role` - пользователи и их роли
- `Artwork`, `Category` - произведения и категории
- `Exhibition` - выставки
- `Comment`, `Like`, `Notification` - взаимодействие пользователей
- `ImageHash` - признаки изображений

#### **Integration Layer**
- `MinIO Client` - интеграция с MinIO для управления файлами
- `ML Engine Client` - HTTP клиент для обращения к Python ML-сервису
- `DTO Objects` - объекты передачи данных

#### **Utilities & Security**
- `Validation/Utility` - валидация и утилиты
- `CustomUserDetailsService` - Spring Security

---

### 4. **C4_Deployment_Diagram.puml** - Диаграмма развертывания
Показывает как компоненты развернуты в окружение.

**Узлы развертывания:**
- **User Device** - устройство пользователя (Windows/macOS/Linux)
  - Web Browser

- **Application Server** - серверная машина
  - Java Runtime (JDK 11)
  - Spring MVC Application (WAR)

- **ML Server** - сервер для ML-сервиса
  - Python Runtime (Python 3.8+)
  - ML Recommendation Engine

- **Storage Server** - сервер хранилища
  - MinIO Container (Docker)
  - MinIO Instance (S3-compatible)

- **Database Server** - сервер БД
  - MySQL Container (Docker)
  - MySQL 8.0

---

## 🚀 Как использовать диаграммы

### Просмотр в PlantUML Editor
1. Откройте [PlantUML Online Editor](http://www.plantuml.com/plantuml/uml/)
2. Скопируйте содержимое любого файла `.puml`
3. Вставьте в редактор и просмотрите диаграмму

### Просмотр в VS Code
1. Установите расширение "PlantUML" (jebbs.plantuml)
2. Откройте файл `.puml`
3. Нажмите `Alt+D` для предпросмотра

### Экспорт диаграмм
```bash
# Экспорт в PNG
plantuml -tpng C4_Context_Diagram.puml

# Экспорт в SVG
plantuml -tsvg C4_Container_Diagram.puml

# Экспорт в PDF
plantuml -tpdf C4_Component_Diagram.puml
```

---

## 📐 Нотация C4 Model

**C4 Model** состоит из четырех уровней абстракции:

1. **Context** (Контекст) - Система и взаимодействие с пользователями и внешними системами
2. **Container** (Контейнеры) - Основные компоненты, сервисы, базы данных
3. **Component** (Компоненты) - Внутренняя структура контейнера
4. **Code** (Код) - Классы и функции (опционально)

---

## 🔑 Легенда

- **Person** (👥) - пользователь системы
- **System** (🖼️) - основная система
- **System_Ext** (📦) - внешняя система
- **Container** - контейнер (приложение, БД, сервис)
- **Component** - компонент внутри контейнера
- **Rel** (стрелка) - связь/взаимодействие

---

## 📝 Роли пользователей

### Посетитель
- Просмотр выставок и произведений
- Просмотр категорий
- Чтение комментариев
- **Нет доступа**: создание, редактирование, удаление

### Художник
- Все права Посетителя
- Загрузка произведений искусства
- Создание и управление выставками
- Добавление комментариев и лайков
- Управление собственным профилем
- **Нет доступа**: модерирование, администрирование

### Администратор
- Все права Художника
- Модерирование контента (одобрение/отклонение произведений)
- Управление категориями
- Управление пользователями
- Просмотр статистики и логов
- Удаление контента при нарушениях

---

## 🏗️ Архитектурные решения

### Backend
- **Framework**: Spring MVC 5.3.20
- **Java Version**: 11
- **Database**: MySQL 8.0.33 с Hibernate ORM
- **Security**: Spring Security 5.7.1
- **Templating**: Thymeleaf 3.0.15

### Frontend
- **Template Engine**: Thymeleaf (Server-side rendering)
- **Styling**: CSS3
- **Markup**: HTML5

### File Storage
- **Solution**: MinIO (S3-compatible)
- **Usage**: Хранение изображений произведений

### ML Recommendation
- **Framework**: OpenCLIP
- **Language**: Python 3.8+
- **API**: REST API через HTTP
- **Features**: Автоматическое получение признаков изображений и рекомендации похожих произведений

### Deployment
- **Web Server**: Apache Tomcat / Jetty
- **Storage**: Docker Container (MinIO)
- **Database**: Docker Container (MySQL)
- **ML Service**: Python process / Docker Container

---

## 📚 Дополнительные ресурсы

- [C4 Model Official](https://c4model.com/)
- [PlantUML Documentation](https://plantuml.com/)
- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [MinIO Documentation](https://docs.min.io/)

---

**Версия документации**: 1.0  
**Дата создания**: 2026-05-19  
**Статус**: ✅ Актуально
