package com.example.assignment5;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class Weather {
    public final String dayOfWeek;
    public final String minTemp;
    public final String maxTemp;
    public final String humidity;
    public final String description;
    public final String iconURL;

    // Constructor
    public Weather(String date, double minTemp, double maxTemp, double humidity, String description, String iconURL) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(0);

        this.dayOfWeek = convertDateToDay(date);
        this.minTemp = numberFormat.format(minTemp) + "\u00B0F";
        this.maxTemp = numberFormat.format(maxTemp) + "\u00B0F";
        this.humidity = NumberFormat.getPercentInstance().format(humidity / 100);
        this.description = description;
        this.iconURL = "https:" + iconURL;
    }

    private static String convertDateToDay(String date) {
        LocalDate localDate = LocalDate.parse(date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy", Locale.getDefault());
        return localDate.format(formatter);
    }
}
