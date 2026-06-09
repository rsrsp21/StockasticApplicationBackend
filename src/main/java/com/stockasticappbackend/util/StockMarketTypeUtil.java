package com.stockasticappbackend.util;

import java.util.Locale;
import java.util.Set;

import com.stockasticappbackend.model.entity.Stock;

public final class StockMarketTypeUtil {

    private static final Set<String> DOMESTIC_EXCHANGES = Set.of("NSE", "BSE");

    private StockMarketTypeUtil() {
    }

    public static boolean isInternational(Stock stock) {
        if (stock == null || stock.getExchange() == null) {
            return false;
        }
        return !DOMESTIC_EXCHANGES.contains(stock.getExchange().trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isDomestic(Stock stock) {
        return !isInternational(stock);
    }
}
