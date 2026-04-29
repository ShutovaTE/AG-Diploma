package com.example.vag.repository;

import com.example.vag.model.Notification;
import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop5ByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndReadFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsReadByUser(@Param("user") User user);

    long deleteByIdAndUser(Long id, User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.id IN :ids")
    int deleteByUserAndIdIn(@Param("user") User user, @Param("ids") Collection<Long> ids);
}
