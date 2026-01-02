workspace "VAG - Virtual Art Gallery" "Диаграмма контейнеров системы виртуальной художественной галереи" {

    model {
        // Пользователи
        artist = person "Художник" "Создает и публикует художественные работы."
        visitor = person "Посетитель" "Просматривает выставки и работы."
        admin = person "Администратор" "Модерирует контент."

        // Основная система VAG
        vagSystem = softwareSystem "VAG (Virtual Art Gallery)" "Платформа для публикации и просмотра художественных работ." {

            // Веб-интерфейсы
            webApp = container "Веб-приложение" "Spring Boot веб-приложение для художников и админов." "Java/Spring Boot"

            // Мобильные приложения
            mobileApp = container "Мобильное приложение" "Android приложение с REST API клиентом." "Kotlin/Android"

            // Backend компоненты
            exhibitionService = container "Сервис выставок" "Управляет выставками и коллекциями работ." "Spring Boot Service"
            artworkService = container "Сервис работ" "Управляет художественными работами." "Spring Boot Service"
            userService = container "Сервис пользователей" "Управляет пользователями и аутентификацией." "Spring Boot Service"
            moderationService = container "Сервис модерации" "Обрабатывает модерацию контента." "Spring Boot Service"

            // Нейронные сети
            recommendationEngine = container "Рекомендательная система" "ML сервис для персонализации контента." "Python/FastAPI"
            imageAnalyzer = container "Анализатор изображений" "ML сервис для анализа и категоризации изображений." "Python/FastAPI"

            // Базы данных
            mainDatabase = container "Основная БД" "MySQL база данных для основного контента." "MySQL"
            userDatabase = container "БД пользователей" "MySQL база данных для профилей." "MySQL"

            // Внешние сервисы
            fileStorage = container "Файловое хранилище" "Хранилище изображений работ." "Local/AWS S3"
        }

        // Связи пользователей
        artist -> webApp "Создает работы через веб"
        artist -> mobileApp "Создает работы через мобильное"
        visitor -> mobileApp "Просматривает контент"
        admin -> webApp "Модерирует контент"

        // API взаимодействия
        webApp -> exhibitionService "REST API"
        webApp -> artworkService "REST API"
        webApp -> userService "REST API"
        webApp -> moderationService "REST API"

        mobileApp -> exhibitionService "REST API"
        mobileApp -> artworkService "REST API"
        mobileApp -> userService "REST API"

        // Взаимодействие сервисов
        exhibitionService -> mainDatabase "CRUD операции"
        artworkService -> mainDatabase "CRUD операции"
        userService -> userDatabase "CRUD операции"
        moderationService -> mainDatabase "Операции модерации"

        // Интеграция с ИИ
        artworkService -> imageAnalyzer "Анализ изображений при загрузке"
        mobileApp -> recommendationEngine "Получение рекомендаций"

        recommendationEngine -> mainDatabase "Анализ предпочтений"
        imageAnalyzer -> mainDatabase "Сохранение результатов анализа"

        // Хранение файлов
        artworkService -> fileStorage "Загрузка/скачивание изображений"
        exhibitionService -> fileStorage "Изображения превью выставок"
    }

    views {
        container vagSystem {
            include *
            autolayout lr
            description "Диаграмма контейнеров системы VAG"
        }

        theme default
    }
}
