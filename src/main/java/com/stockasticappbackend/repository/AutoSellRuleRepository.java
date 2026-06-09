package com.stockasticappbackend.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.AutoSellRule;
import com.stockasticappbackend.model.entity.Stock;

@Repository
public interface AutoSellRuleRepository extends JpaRepository<AutoSellRule, Long> {

    List<AutoSellRule> findByUser(AppUser user);

    Page<AutoSellRule> findByUser(AppUser user, Pageable pageable);

    Page<AutoSellRule> findByUserAndIsActive(AppUser user, boolean isActive, Pageable pageable);

    // Note: We need to find ALL active rules for a stock to check them during price updates
    List<AutoSellRule> findByStockAndIsActiveTrue(Stock stock);

    List<AutoSellRule> findByUserAndStockAndIsActiveTrue(AppUser user, Stock stock);
    
    // Check if rule exists for user and stock
    boolean existsByUserAndStockAndIsActiveTrue(AppUser user, Stock stock);

    List<AutoSellRule> findByIsActiveTrue();
}
