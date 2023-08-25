package com.github.xesam.printing.cloud;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class QueryOption {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String date;

    public QueryOption() {
    }

    public String getDate() {
        return date;
    }

    public QueryOption setDate(String date) {
        this.date = date;
        return this;
    }

    public QueryOption setDate(LocalDate date) {
        this.date = date.format(dateTimeFormatter);
        return this;
    }

    @Override
    public String toString() {
        return "QueryOption{" +
                "date='" + date + '\'' +
                '}';
    }
}
