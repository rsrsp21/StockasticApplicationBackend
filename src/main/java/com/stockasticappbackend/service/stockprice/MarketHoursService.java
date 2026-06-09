package com.stockasticappbackend.service.stockprice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to handle Indian Stock Market trading hours and holidays
 */
@Service
@Slf4j
public class MarketHoursService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    // NSE/BSE Holidays for 2026 (update annually)
    // Source: NSE India holiday calendar
    private static final Set<LocalDate> MARKET_HOLIDAYS_2026 = Set.of(
            LocalDate.of(2026, 1, 26),  // Republic Day
            LocalDate.of(2026, 3, 3),   // Holi
            LocalDate.of(2026, 3, 26),  // Shri Ram Navami
            LocalDate.of(2026, 3, 31),  // Shri Mahavir Jayanti
            LocalDate.of(2026, 4, 3),   // Good Friday
            LocalDate.of(2026, 4, 14),  // Dr. Baba Saheb Ambedkar Jayanti
            LocalDate.of(2026, 5, 1),   // Maharashtra Day
            LocalDate.of(2026, 5, 28),  // Bakri Id
            LocalDate.of(2026, 6, 26),  // Muharram
            LocalDate.of(2026, 9, 14),  // Ganesh Chaturthi
            LocalDate.of(2026, 10, 2),  // Mahatma Gandhi Jayanti
            LocalDate.of(2026, 10, 20), // Dussehra
            LocalDate.of(2026, 11, 10), // Diwali-Balipratipada
            LocalDate.of(2026, 11, 24), // Guru Nanak Jayanti
            LocalDate.of(2026, 12, 25)  // Christmas
    );

    /**
     * Get current time in IST
     */
    public ZonedDateTime getCurrentISTTime() {
        return ZonedDateTime.now(IST_ZONE);
    }

    /**
     * Check if market is currently open
     * Market hours: 9:15 AM to 3:30 PM IST, Monday to Friday, excluding holidays
     */
    @Cacheable(value = "marketStatus", key = "'isOpen'")
    public boolean isMarketOpen() {
        ZonedDateTime now = getCurrentISTTime();
        LocalTime currentTime = now.toLocalTime();
        LocalDate currentDate = now.toLocalDate();

        // Check if it's a trading day
        if (!isTradingDay(currentDate)) {
            return false;
        }

        // Check if within market hours
        return !currentTime.isBefore(MARKET_OPEN) && !currentTime.isAfter(MARKET_CLOSE);
    }

    /**
     * Check if the given date is a trading day (not weekend, not holiday)
     */
    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Weekend check
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Holiday check
        if (MARKET_HOLIDAYS_2026.contains(date)) {
            return false;
        }

        return true;
    }

    /**
     * Check if today is a trading day
     */
    @Cacheable(value = "marketStatus", key = "'tradingDay'")
    public boolean isTodayTradingDay() {
        return isTradingDay(getCurrentISTTime().toLocalDate());
    }

    /**
     * Check if it's market opening time (9:15 AM)
     */
    public boolean isMarketOpeningTime() {
        ZonedDateTime now = getCurrentISTTime();
        LocalTime currentTime = now.toLocalTime();

        // Check if current time is between 9:15:00 and 9:19:59 (5-minute window)
        return !currentTime.isBefore(MARKET_OPEN) &&
                currentTime.isBefore(MARKET_OPEN.plusMinutes(5));
    }

    /**
     * Check if it's before market open today
     */
    public boolean isBeforeMarketOpen() {
        LocalTime currentTime = getCurrentISTTime().toLocalTime();
        return currentTime.isBefore(MARKET_OPEN);
    }

    /**
     * Check if it's after market close today
     */
    public boolean isAfterMarketClose() {
        LocalTime currentTime = getCurrentISTTime().toLocalTime();
        return currentTime.isAfter(MARKET_CLOSE);
    }

    /**
     * Get market open time
     */
    public LocalTime getMarketOpenTime() {
        return MARKET_OPEN;
    }

    /**
     * Get market close time
     */
    public LocalTime getMarketCloseTime() {
        return MARKET_CLOSE;
    }

    /**
     * Get the last trading day (for weekends/holidays, returns the previous trading
     * day)
     */
    public LocalDate getLastTradingDay() {
        LocalDate date = getCurrentISTTime().toLocalDate();

        // If today is a trading day and market has opened, return today
        if (isTradingDay(date) && !isBeforeMarketOpen()) {
            return date;
        }

        // Otherwise, find the previous trading day
        date = date.minusDays(1);
        while (!isTradingDay(date)) {
            date = date.minusDays(1);
        }
        return date;
    }
}
