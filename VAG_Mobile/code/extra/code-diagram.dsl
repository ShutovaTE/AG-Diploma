@startuml VAG Database Class Diagram
!theme plain

' Основные сущности
class User {
    +Long id
    +String username
    +String email
    +String password
    +String description
    +Role role
    +LocalDateTime createdAt
    +LocalDateTime updatedAt
    --
    +Collection<? extends GrantedAuthority> getAuthorities()
    +String getPassword()
    +String getUsername()
    +boolean isAccountNonExpired()
    +boolean isAccountNonLocked()
    +boolean isCredentialsNonExpired()
    +boolean isEnabled()
    +boolean hasRole(String roleName)
}

class Role {
    +Long id
    +RoleName name
    --
    +enum RoleName {
        ADMIN
        ARTIST
    }
}

class Artwork {
    +Long id
    +String title
    +String description
    +String imagePath
    +LocalDate dateCreation
    +String status
    +int likes
    +int views
    --
    +enum ArtworkStatus {
        PENDING
        APPROVED
        REJECTED
    }
}

class Exhibition {
    +Long id
    +String title
    +String description
    +String imageUrl
    +boolean authorOnly
    +LocalDate createdAt
}

class Category {
    +Long id
    +String name
    +String description
    +Long approvedArtworksCount
}

class Comment {
    +Long id
    +String content
    +LocalDateTime dateCreated
}

class Like {
    +Long id
}

' Отношения между сущностями
User ||--o{ Artwork : создает
User ||--o{ Exhibition : создает
User ||--o{ Comment : пишет
User ||--o{ Like : ставит

User }o--|| Role : имеет

Artwork ||--o{ Comment : имеет
Artwork ||--o{ Like : имеет

Artwork }o--o{ Category : относится к
Artwork }o--o{ Exhibition : входит в

Exhibition }o--|| User : принадлежит
Artwork }o--|| User : принадлежит
Comment }o--|| User : принадлежит
Comment }o--|| Artwork : принадлежит
Like }o--|| User : принадлежит
Like }o--|| Artwork : принадлежит

' Подписи отношений
note right of User : Пользователь системы\n(художник/администратор)
note right of Artwork : Художественная работа
note right of Exhibition : Выставка/коллекция работ
note right of Category : Категория работ
note right of Comment : Комментарий к работе
note right of Like : Лайк работы

@enduml
