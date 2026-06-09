package com.stockasticappbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.SipTransaction;

@Repository
public interface SipTransactionRepository extends JpaRepository<SipTransaction, Long> {

    // Ideally, we filter by the user associated with the SIP
    Page<SipTransaction> findBySipUserOrderByExecutionDateDesc(AppUser user, Pageable pageable);
}
