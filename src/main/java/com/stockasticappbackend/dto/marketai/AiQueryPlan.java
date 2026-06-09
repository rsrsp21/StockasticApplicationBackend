package com.stockasticappbackend.dto.marketai;

import java.util.List;

import lombok.Data;

@Data
public class AiQueryPlan {

    private AiQueryAction action = AiQueryAction.SNAPSHOT;
    private String symbol;
    private String comparisonSymbol;
    private String time;
    private String comparisonTime;
    private String comparisonSymbolTime;
    private List<String> symbols;
    private String sector;
    private String direction;
    private String rationale;
}
