workspace "VAG - Virtual Art Gallery" "Диаграмма компонентов для ExhibitionService" {

    model {
        // Основная система
        vagSystem = softwareSystem "VAG (Virtual Art Gallery)" "Платформа для художественных работ." {

            // Backend API
            backendAPI = container "Backend API" "Spring Boot REST API." "Java/Spring Boot" {

                // Компоненты ExhibitionService
                exhibitionController = component "MobileExhibitionController" "REST контроллер для управления выставками в мобильном API." "Spring MVC Controller"
                exhibitionService = component "ExhibitionService" "Бизнес-логика управления выставками." "Spring Service"
                exhibitionRepository = component "ExhibitionRepository" "Доступ к данным выставок." "Spring Data JPA"

                // Компоненты для работ
                artworkController = component "MobileArtworkController" "REST контроллер для управления работами." "Spring MVC Controller"
                artworkService = component "ArtworkService" "Бизнес-логика управления работами." "Spring Service"
                artworkRepository = component "ArtworkRepository" "Доступ к данным работ." "Spring Data JPA"

                // Компоненты пользователей
                userService = component "UserService" "Управление пользователями." "Spring Service"
                userRepository = component "UserRepository" "Доступ к данным пользователей." "Spring Data JPA"

                // Компоненты модерации
                categoryService = component "CategoryService" "Управление категориями работ." "Spring Service"
                fileUploadUtil = component "FileUploadUtil" "Утилиты для загрузки файлов." "Spring Component"

                // Нейронные сети
                recommendationEngine = component "Рекомендательная система" "Анализирует предпочтения пользователей." "Python ML Service"
                imageAnalyzer = component "Анализатор изображений" "Категоризирует и проверяет изображения." "Python ML Service"

                // Базы данных
                mainDatabase = component "Основная БД" "MySQL база данных." "MySQL"
                userDatabase = component "БД пользователей" "MySQL база данных." "MySQL"
            }
        }

        // Связи компонентов Exhibition
        exhibitionController -> exhibitionService "Использует для бизнес-логики"
        exhibitionService -> exhibitionRepository "CRUD операции с выставками"
        exhibitionService -> artworkService "Управление работами в выставках"
        exhibitionService -> userService "Проверка прав доступа"

        // Связи с работами
        exhibitionController -> artworkController "Получение работ выставки"
        artworkController -> artworkService "Бизнес-логика работ"
        artworkService -> artworkRepository "CRUD операции с работами"

        // Связи с пользователями
        exhibitionController -> userService "Аутентификация"
        artworkController -> userService "Аутентификация"
        userService -> userRepository "Доступ к пользователям"

        // Модерация и категории
        artworkService -> categoryService "Управление категориями"
        artworkService -> fileUploadUtil "Загрузка изображений"

        // Интеграция с ИИ
        artworkService -> imageAnalyzer "Анализ загружаемых изображений"
        exhibitionController -> recommendationEngine "Рекомендации выставок"

        // Доступ к данным
        exhibitionRepository -> mainDatabase "Чтение/запись выставок"
        artworkRepository -> mainDatabase "Чтение/запись работ"
        userRepository -> userDatabase "Чтение/запись пользователей"

        categoryService -> mainDatabase "Доступ к категориям"
    }

    views {
        component backendAPI {
            include *
            autolayout lr
            description "Диаграмма компонентов ExhibitionService"
        }

        theme default
    }
}
