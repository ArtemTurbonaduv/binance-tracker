package org.example.binancetracker.service;

import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface PriceService {
    PriceDto getLatestPrice(String symbol);
    AveragePriceResponse getAveragePrice(String symbol, Instant from, Instant to);
    PageResponse<PriceDto> getPrices(String symbol, Instant from, Instant to, Pageable pageable);
    TrendResponse calculateTrend(String symbol, Instant from, Instant to);
}