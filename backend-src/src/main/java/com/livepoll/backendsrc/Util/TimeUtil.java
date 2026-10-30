package com.livepoll.backendsrc.Util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    public static Instant getCurrentDate(){
        return Instant.now();
    }
    public static Instant getHourFromNow(){
        return Instant.now().plusMillis(3600 * 1000);
    }
    public static Instant getWeekFromNow(){
        return Instant.now().plusMillis(604800 * 1000);
    }

    public static String getFilePrefix() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
