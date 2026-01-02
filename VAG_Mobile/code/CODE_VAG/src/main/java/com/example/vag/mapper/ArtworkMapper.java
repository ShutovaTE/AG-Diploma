package com.example.vag.mapper;

import com.example.vag.dto.ArtworkDTO;
import com.example.vag.dto.CategoryDTO;
import com.example.vag.dto.UserDTO;
import com.example.vag.dto.CommentDTO;
import com.example.vag.model.Artwork;
import com.example.vag.model.Category;
import com.example.vag.model.User;
import com.example.vag.model.Comment;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ArtworkMapper {

    public ArtworkDTO toDTO(Artwork artwork) {
        if (artwork == null) {
            return null;
        }

        ArtworkDTO dto = new ArtworkDTO();
        dto.setId(artwork.getId());
        dto.setTitle(artwork.getTitle());
        dto.setDescription(artwork.getDescription());
        dto.setImagePath(artwork.getImagePath());
        dto.setStatus(artwork.getStatus());
        dto.setLikes(artwork.getLikes());
        dto.setViews(artwork.getViews());
        dto.setDateCreation(artwork.getDateCreation());

        // ВАЖНО: Добавляем пользователя
        if (artwork.getUser() != null) {
            UserDTO userDTO = toUserDTO(artwork.getUser());
            dto.setUser(userDTO);
            System.out.println("Mapper: UserDTO created - " + userDTO.getUsername() +
                    ", description: " + userDTO.getDescription());
        }

        // ИСПРАВЛЕНО: Правильно добавляем категории - инициализируем коллекцию
        if (artwork.getCategories() != null) {
            try {
                // Принудительно инициализируем коллекцию
                Hibernate.initialize(artwork.getCategories());

                List<CategoryDTO> categoryDTOs = artwork.getCategories().stream()
                        .map(this::toCategoryDTO)
                        .collect(Collectors.toList());
                dto.setCategories(categoryDTOs);
                System.out.println("Mapper: Added " + categoryDTOs.size() + " categories");
            } catch (Exception e) {
                System.out.println("Error initializing categories: " + e.getMessage());
                dto.setCategories(new ArrayList<>());
            }
        } else {
            System.out.println("Mapper: No categories found for artwork");
            dto.setCategories(new ArrayList<>()); // Устанавливаем пустой список
        }

        // Добавляем комментарии
        if (artwork.getComments() != null) {
            try {
                Hibernate.initialize(artwork.getComments());
                dto.setComments(artwork.getComments().stream()
                        .map(this::toCommentDTO)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                System.out.println("Error initializing comments: " + e.getMessage());
                dto.setComments(new ArrayList<>());
            }
        }

        // Добавляем информацию о лайке
        dto.setLiked(artwork.getLiked());

        System.out.println("Mapper: Final ArtworkDTO - " + dto.getTitle() +
                ", User: " + (dto.getUser() != null ? dto.getUser().getUsername() : "null") +
                ", User Description: " + (dto.getUser() != null ? dto.getUser().getDescription() : "null") +
                ", Categories: " + (dto.getCategories() != null ? dto.getCategories().size() : 0) +
                ", Image: " + dto.getImagePath() +
                ", Description: " + dto.getDescription());

        return dto;
    }

    public ArtworkDTO toSimpleDTO(Artwork artwork) {
        if (artwork == null) {
            return null;
        }

        ArtworkDTO dto = new ArtworkDTO();
        dto.setId(artwork.getId());
        dto.setTitle(artwork.getTitle());
        dto.setDescription(artwork.getDescription());
        dto.setImagePath(artwork.getImagePath());
        dto.setStatus(artwork.getStatus());
        dto.setLikes(artwork.getLikes());
        dto.setViews(artwork.getViews());
        dto.setDateCreation(artwork.getDateCreation());

        // ВАЖНО: Добавляем пользователя даже в простом DTO
        if (artwork.getUser() != null) {
            dto.setUser(toSimpleUserDTO(artwork.getUser()));
        }

        // ИСПРАВЛЕНО: Добавляем категории даже в простом DTO
        if (artwork.getCategories() != null) {
            try {
                Hibernate.initialize(artwork.getCategories());
                List<CategoryDTO> categoryDTOs = artwork.getCategories().stream()
                        .map(this::toCategoryDTO)
                        .collect(Collectors.toList());
                dto.setCategories(categoryDTOs);
            } catch (Exception e) {
                dto.setCategories(new ArrayList<>());
            }
        } else {
            dto.setCategories(new ArrayList<>());
        }

        return dto;
    }

    public List<ArtworkDTO> toDTOList(List<Artwork> artworks) {
        return artworks.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ArtworkDTO> toSimpleDTOList(List<Artwork> artworks) {
        return artworks.stream()
                .map(this::toSimpleDTO)
                .collect(Collectors.toList());
    }

    public UserDTO toUserDTO(User user) {
        if (user == null) {
            System.out.println("Mapper: User is null in toUserDTO");
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername() != null ? user.getUsername() : "Unknown User");
        dto.setEmail(user.getEmail() != null ? user.getEmail() : "N/A");

        // ДОБАВЛЕНО: Устанавливаем description
        dto.setDescription(user.getDescription() != null ? user.getDescription() : "");

        if (user.getRole() != null) {
            dto.setRole(user.getRole().getName().name());
        } else {
            dto.setRole("UNKNOWN");
        }

        // ДОБАВЛЕНО: Добавляем количество публикаций
        if (user.getArtworks() != null) {
            dto.setArtworksCount(user.getArtworks().size());
        } else {
            dto.setArtworksCount(0);
        }

        System.out.println("Mapper: UserDTO - " + dto.getUsername() +
                ", email: " + dto.getEmail() +
                ", description: " + dto.getDescription() +
                ", artworksCount: " + dto.getArtworksCount());
        return dto;
    }

    public UserDTO toSimpleUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        // ДОБАВЛЕНО: Добавляем description
        dto.setDescription(user.getDescription() != null ? user.getDescription() : "");

        return dto;
    }

    public CategoryDTO toCategoryDTO(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        Long count = category.getApprovedArtworksCount();
        dto.setApprovedArtworksCount(count != null ? count : 0L);

        return dto;
    }

    public CommentDTO toCommentDTO(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setDateCreated(comment.getDateCreated());

        if (comment.getUser() != null) {
            dto.setUser(toSimpleUserDTO(comment.getUser()));
        }

        return dto;
    }

    public List<CategoryDTO> toCategoryDTOList(List<Category> categories) {
        return categories.stream()
                .map(this::toCategoryDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> toUserDTOList(List<User> users) {
        return users.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    // ДОБАВЛЕНО: Метод для получения DTO художников с количеством публикаций
    public List<UserDTO> toArtistsWithCountDTOList(List<User> users) {
        return users.stream()
                .map(user -> {
                    UserDTO dto = toUserDTO(user);
                    // Устанавливаем количество публикаций
                    if (user.getArtworks() != null) {
                        dto.setArtworksCount(user.getArtworks().size());
                    } else {
                        dto.setArtworksCount(0);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}