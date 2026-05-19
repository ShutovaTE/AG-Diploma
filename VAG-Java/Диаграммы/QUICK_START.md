# 🚀 Быстрый старт просмотра C4 диаграмм

## 📍 Где находятся диаграммы?

Все диаграммы находятся в папке: `Диаграммы/`

Файлы:
- ✅ `C4_Context_Diagram.puml` - Диаграмма контекста
- ✅ `C4_Container_Diagram.puml` - Диаграмма контейнеров
- ✅ `C4_Component_Diagram.puml` - Диаграмма компонентов (детальная)
- ✅ `C4_Deployment_Diagram.puml` - Диаграмма развертывания
- ✅ `C4_DIAGRAMS_README.md` - Полная документация

---

## 🎯 Вариант 1: Просмотр онлайн (самый быстрый)

### PlantUML Online Editor
1. Откройте [http://www.plantuml.com/plantuml/uml/](http://www.plantuml.com/plantuml/uml/)
2. Скопируйте содержимое файла `.puml`
3. Вставьте в редактор
4. Диаграмма отобразится автоматически!

**Преимущества:**
- ✅ Никаких установок
- ✅ Мгновенный просмотр
- ✅ Экспорт в PNG, SVG, PDF

---

## 🎯 Вариант 2: VS Code (рекомендуется)

### Установка расширения
1. Откройте VS Code
2. Перейдите на вкладку Extensions (Ctrl+Shift+X)
3. Поищите "PlantUML"
4. Установите расширение [PlantUML](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml) от jebbs

### Просмотр диаграмм
1. Откройте любой файл `.puml` в папке `Диаграммы`
2. Нажмите **Alt + D** (или Ctrl+Shift+P → "PlantUML: Preview Current Diagram")
3. Диаграмма откроется в предпросмотре рядом с редактором

**Горячие клавиши:**
- `Alt + D` - открыть/закрыть предпросмотр
- `Ctrl+Shift+P` → "Export" - экспортировать в PNG/SVG/PDF

---

## 🎯 Вариант 3: Командная строка (для автоматизации)

### Установка PlantUML
```bash
# Windows (через Chocolatey)
choco install plantuml

# macOS (через Homebrew)
brew install plantuml

# Linux (через apt)
sudo apt-get install plantuml
```

### Экспорт диаграмм
```bash
# Экспорт всех диаграмм в PNG
cd Диаграммы
plantuml -tpng *.puml

# Экспорт конкретной диаграммы в SVG
plantuml -tsvg C4_Context_Diagram.puml

# Экспорт в PDF
plantuml -tpdf C4_Component_Diagram.puml
```

---

## 📊 Рекомендуемый порядок изучения

### Для руководителя/менеджера:
1. **Context Diagram** - поймет взаимодействие с пользователями
2. **Container Diagram** - увидит основные компоненты
3. **Deployment Diagram** - поймет инфраструктуру

### Для архитектора:
1. **Context Diagram** - контекст системы
2. **Container Diagram** - архитектура на уровне контейнеров
3. **Component Diagram** - детальная структура приложения
4. **Deployment Diagram** - инфраструктура и развертывание

### Для разработчика:
1. **Component Diagram** - структура кода и компоненты
2. **Container Diagram** - взаимодействие контейнеров
3. **Deployment Diagram** - как всё развернуто
4. **Context Diagram** - общий контекст

---

## 🔍 Что показано в каждой диаграмме?

### 1️⃣ Context Diagram
```
Показывает:
- 3 типа пользователей (Посетитель, Художник, Администратор)
- Главную систему VAG
- 3 внешние системы (MySQL, MinIO, ML Engine)
- Взаимодействие между ними
```

### 2️⃣ Container Diagram
```
Показывает:
- Web Browser (фронтенд)
- Spring MVC Application (основное приложение)
- MySQL Database
- MinIO Storage
- Python ML Service
- Все связи между контейнерами
```

### 3️⃣ Component Diagram (САМАЯ ДЕТАЛЬНАЯ)
```
Показывает:
- 9 Controllers (Web Layer)
- 8 Services (Business Logic Layer)
- 8 Repositories (Data Access Layer)
- 5 Entity Groups (Model Layer)
- Integration Components (MinIO, ML)
- Все связи между компонентами
```

### 4️⃣ Deployment Diagram
```
Показывает:
- User Device (Web Browser)
- Application Server (Java + Spring)
- ML Server (Python)
- Storage Server (MinIO)
- Database Server (MySQL)
- Протоколы и порты взаимодействия
```

---

## 🔐 Роли пользователей (на диаграммах обозначены как Persons)

| Роль | Описание | Права |
|------|---------|-------|
| **Посетитель** 👥 | Незарегистрированный пользователь | Просмотр выставок, произведений, категорий |
| **Художник** 🎨 | Авторизованный пользователь-автор | Загрузка произведений, создание выставок, комментирование |
| **Администратор** 👨‍💼 | Управляющий пользователь | Модерирование, управление категориями, администрирование |

---

## 💡 Ключевые архитектурные компоненты

### Backend (Spring MVC)
- **Controllers** → обработка HTTP запросов
- **Services** → бизнес-логика
- **Repositories** → доступ к БД через Hibernate
- **Entities** → модели данных

### Data Storage
- **MySQL** → основные данные (пользователи, произведения, выставки, комментарии)
- **MinIO** → файлы изображений (S3-compatible)

### ML Integration
- **RecommendationService** → координирует рекомендации
- **ImageFeatureService** → обработка признаков изображений
- **Python ML Engine** → OpenCLIP для анализа изображений

### Security
- **Spring Security** → аутентификация и авторизация
- **CustomUserDetailsService** → загрузка деталей пользователя

---

## 📝 Примеры использования

### Как художник загружает произведение?
1. Художник заходит на сайт (Web Browser)
2. Открывает страницу загрузки (`/upload`)
3. `UploadController` → `ArtworkService` → сохраняет в MySQL
4. Файл изображения → `MinIO Client` → `MinIO Storage`
5. `ImageFeatureService` → `ML Engine Client` → получает признаки от Python
6. Сохраняет признаки в `ImageHashRepository`

### Как система рекомендует похожие произведения?
1. Пользователь просматривает произведение
2. `ArtworkController` вызывает `RecommendationController`
3. `RecommendationController` → `RecommendationService`
4. `RecommendationService` → `ML Engine Client` (HTTP запрос к Python)
5. Python ML Engine анализирует изображение (OpenCLIP)
6. Возвращает список похожих произведений
7. `RecommendationDTO` отправляет результаты в Web Browser

---

## 🛠️ Технологический стек (из диаграмм)

| Слой | Технология | Версия |
|------|-----------|--------|
| Frontend | HTML5, CSS3, JavaScript | - |
| Template Engine | Thymeleaf | 3.0.15 |
| Web Framework | Spring MVC | 5.3.20 |
| Language | Java | 11 |
| ORM | Hibernate | 5.6.9 |
| Database | MySQL | 8.0.33 |
| Security | Spring Security | 5.7.1 |
| File Storage | MinIO | - |
| ML Framework | OpenCLIP | - |
| ML Language | Python | 3.8+ |
| Build Tool | Maven | - |

---

## ✨ Особенности диаграмм

✅ **Полная C4 нотация** - используется стандартная нотация C4 Model  
✅ **Русский язык** - все надписи на русском для удобства  
✅ **Детальное описание** - каждый компонент имеет назначение  
✅ **Актуальность** - отражают текущую архитектуру VAG  
✅ **Интеграция MinIO** - показывают использование S3-compatible storage  
✅ **ML Integration** - полная интеграция Python ML движка  
✅ **Три роли пользователей** - Посетитель, Художник, Администратор  

---

## 📖 Дополнительные материалы

- [C4 Model Official Website](https://c4model.com/) - официальная документация
- [PlantUML Documentation](https://plantuml.com/starting) - синтаксис PlantUML
- [Spring Architecture](https://spring.io/projects/spring-framework) - Spring Framework
- [MinIO Documentation](https://docs.min.io/) - MinIO ObjectStorage
- [OpenCLIP GitHub](https://github.com/OpenAI/CLIP) - ML Model

---

## ❓ Часто задаваемые вопросы

**Q: Может ли Посетитель загружать произведения?**  
A: Нет, только авторизованный Художник может загружать произведения.

**Q: Где хранятся изображения?**  
A: В MinIO (S3-compatible Object Storage).

**Q: Как работают рекомендации?**  
A: Python ML Engine анализирует изображения с помощью OpenCLIP и возвращает похожие произведения.

**Q: Может ли Администратор удалять произведения?**  
A: Да, Администратор имеет доступ к ModerationService.

**Q: На каком языке код?**  
A: Backend на Java (Spring), Frontend на HTML/CSS/JavaScript (Thymeleaf), ML на Python.

---

## 📞 Поддержка

Если у вас есть вопросы по диаграммам:
1. Прочитайте `C4_DIAGRAMS_README.md` - полная документация
2. Посмотрите примеры на [C4 Model Website](https://c4model.com/), примеры)
3. Экспериментируйте с диаграммами в [PlantUML Online](http://www.plantuml.com/plantuml/uml/)

---

**Версия**: 1.0  
**Обновлено**: 19 мая 2026 г.  
**Статус**: ✅ Готово к использованию
