package com.stockasticappbackend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Sip;
import com.stockasticappbackend.model.enums.SipStatus;

@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {

    List<Sip> findByUser(AppUser user);

    @Query("SELECT s FROM Sip s WHERE s.user = :user AND s.stock.stockId = :stockId")
    List<Sip> findByUserAndStockId(@Param("user") AppUser user, @Param("stockId") Long stockId);

    List<Sip> findByStatusAndNextExecutionDateLessThanEqual(SipStatus status, LocalDate date);
}
