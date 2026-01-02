workspace "VAG - Virtual Art Gallery" "Диаграмма контекста системы виртуальной художественной галереи" {

    model {
        // Пользователи системы
        artist = person "Художник" "Создает и публикует художественные работы в галерее."
        visitor = person "Посетитель" "Просматривает выставки и работы в галерее."
        admin = person "Администратор" "Модерирует контент и управляет системой."

        // Основная система VAG
        vagSystem = softwareSystem "VAG (Virtual Art Gallery)" "Платформа для публикации и просмотра художественных работ." {

            // Веб-приложение
            webApp = container "Веб-приложение" "Интерфейс для художников и администраторов." "Spring Boot Web Application"

            // Мобильное приложение
            mobileApp = container "Мобильное приложение" "Android приложение для просмотра и создания контента." "Android Application"

            // Backend сервисы
            backendAPI = container "Backend API" "REST API для обработки бизнес-логики." "Spring Boot Application"

            // Базы данных
            mainDatabase = container "Основная БД" "Хранит пользователей, работы, выставки." "MySQL Database"
            userDatabase = container "БД пользователей" "Хранит профили пользователей." "MySQL Database"

            // Нейронные сети
            recommendationEngine = container "Рекомендательная система" "Анализирует предпочтения и рекомендует контент." "Python ML Service"
            imageAnalyzer = container "Анализатор изображений" "Категоризирует изображения и проверяет на запрещенный контент." "Python ML Service"
        }

        // Внешние системы
        fileStorage = softwareSystem "Файловое хранилище" "Хранит изображения работ." "AWS S3 / Local Storage"
        notificationService = softwareSystem "Сервис уведомлений" "Отправляет push-уведомления." "Firebase / Custom Service"

        // Связи пользователей с системой
        artist -> webApp "Создает и управляет работами"
        artist -> mobileApp "Создает и управляет работами"
        visitor -> mobileApp "Просматривает выставки и работы"
        admin -> webApp "Модерирует контент"

        // Внутренние связи
        webApp -> backendAPI "HTTP/JSON API"
        mobileApp -> backendAPI "HTTP/JSON API"

        backendAPI -> mainDatabase "Читает/записывает данные"
        backendAPI -> userDatabase "Читает/записывает данные"

        // Интеграция с нейронными сетями
        backendAPI -> recommendationEngine "Получает рекомендации"
        backendAPI -> imageAnalyzer "Анализирует изображения"

        recommendationEngine -> mainDatabase "Получает данные о предпочтениях"
        imageAnalyzer -> mainDatabase "Сохраняет результаты анализа"

        // Внешние интеграции
        backendAPI -> fileStorage "Загружает/скачивает изображения"
        backendAPI -> notificationService "Отправляет уведомления"
    }

    views {
        systemContext vagSystem {
            include *
            autolayout lr
            description "Диаграмма контекста системы VAG"
        }

        theme default
    }
}
