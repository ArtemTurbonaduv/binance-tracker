package org.example.binancetracker.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceAvgPriceResponse(
        int mins,
        BigDecimal price,
        long closeTime
) {
}