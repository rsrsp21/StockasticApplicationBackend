package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockasticappbackend.model.enums.SipTransactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sip_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SipTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sip_id", nullable = false)
    private Sip sip;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private LocalDateTime executionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SipTransactionStatus status;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private BigDecimal price;

    private Integer quantity;
}
