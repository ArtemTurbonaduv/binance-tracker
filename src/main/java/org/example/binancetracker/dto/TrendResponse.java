package org.example.binancetracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TrendResponse(
        String symbol,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate,
        String trend
) {}