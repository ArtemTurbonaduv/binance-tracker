package org.example.binancetracker.dto;

import java.math.BigDecimal;

public record AveragePriceResponse(
        BigDecimal averagePrice
) {}