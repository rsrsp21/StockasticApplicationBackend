package com.stockasticappbackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.StockIndicator;

@Repository
public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {

    Optional<StockIndicator> findByStock_StockId(@Param("stockId") Long stockId);

    List<StockIndicator> findByStock_StockIdIn(@Param("stockIds") Collection<Long> stockIds);
}
