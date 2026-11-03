package org.example.binancetracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceDto(
        String symbol,
        BigDecimal price,
        Instant fetchedAt
) {}