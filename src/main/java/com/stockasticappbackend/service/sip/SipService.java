package com.stockasticappbackend.service.sip;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.stockasticappbackend.dto.sip.SipRequest;
import com.stockasticappbackend.dto.sip.SipResponse;
import com.stockasticappbackend.dto.sip.SipTransactionResponse;
import com.stockasticappbackend.model.enums.SipStatus;

public interface SipService {

    SipResponse createSip(String email, SipRequest request);

    SipResponse updateSip(String email, Long sipId, SipRequest request);

    SipResponse toggleSipStatus(String email, Long sipId, SipStatus status);

    SipResponse getSip(String email, Long sipId);

    List<SipResponse> getUserSips(String email);

    List<SipResponse> getSipsByStock(String email, Long stockId);

    Page<SipTransactionResponse> getSipHistory(String email, Pageable pageable);

    void executeScheduledSips();

    void processDailySips(boolean notifyUpcoming);
}
