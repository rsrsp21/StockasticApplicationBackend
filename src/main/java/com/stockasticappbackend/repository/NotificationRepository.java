package com.stockasticappbackend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get notifications for a user, sorted by date desc
    Page<Notification> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable);

    // Count unread notifications
    long countByUserAndIsReadFalse(AppUser user);

    // Get unread notifications
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(AppUser user);
}
