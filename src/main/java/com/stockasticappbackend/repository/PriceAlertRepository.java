package com.stockasticappbackend.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.PriceAlert;
import com.stockasticappbackend.model.entity.Stock;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByUser(AppUser user);

    Page<PriceAlert> findByUser(AppUser user, Pageable pageable);

    Page<PriceAlert> findByUserAndIsActive(AppUser user, boolean isActive, Pageable pageable);

    // Note: We need to find ALL active alerts for a stock to check them during price updates
    List<PriceAlert> findByStockAndIsActiveTrue(Stock stock);

    List<PriceAlert> findByIsActiveTrue();
}
