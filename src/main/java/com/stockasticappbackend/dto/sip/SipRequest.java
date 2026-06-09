package com.stockasticappbackend.dto.sip;

import com.stockasticappbackend.util.Constants;

import java.time.LocalDate;

import com.stockasticappbackend.model.enums.SipFrequency;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SipRequest {

    @NotNull(message = Constants.STOCK_ID_REQUIRED)
    private Long stockId;

    @NotNull(message = Constants.FREQUENCY_REQUIRED)
    private SipFrequency frequency;

    @NotNull(message = Constants.QUANTITY_REQUIRED)
    @Min(value = 1, message = Constants.QUANTITY_MIN_ONE)
    private Integer quantity;

    @NotNull(message = Constants.START_DATE_REQUIRED)
    @Future(message = Constants.START_DATE_MUST_BE_FUTURE)
    private LocalDate startDate;
}


