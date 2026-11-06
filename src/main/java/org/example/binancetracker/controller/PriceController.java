package org.example.binancetracker.controller;

import lombok.RequiredArgsConstructor;
import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.example.binancetracker.service.PriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @Value("${binance.default-symbol:BNBUSDT}")
    private String defaultSymbol;

    @GetMapping("/latest")
    public ResponseEntity<PriceDto> getLatestPrice(
            @RequestParam(required = false) String symbol
    ) {
        String targetSymbol = (symbol != null && !symbol.isBlank()) ? symbol : defaultSymbol;
        return ResponseEntity.ok(priceService.getLatestPrice(targetSymbol));
    }

    @GetMapping("/average")
    public ResponseEntity<AveragePriceResponse> getAveragePrice(
            @RequestParam(required = false) String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        String targetSymbol = (symbol != null && !symbol.isBlank()) ? symbol : defaultSymbol;
        return ResponseEntity.ok(priceService.getAveragePrice(targetSymbol, from, to));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PriceDto>> getPrices(
            @RequestParam(required = false) String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        String targetSymbol = (symbol != null && !symbol.isBlank()) ? symbol : defaultSymbol;
        return ResponseEntity.ok(priceService.getPrices(targetSymbol, from, to, PageRequest.of(page, size)));
    }

    @GetMapping("/trend")
    public ResponseEntity<TrendResponse> getTrend(
            @RequestParam(required = false) String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        String targetSymbol = (symbol != null && !symbol.isBlank()) ? symbol : defaultSymbol;
        return ResponseEntity.ok(priceService.calculateTrend(targetSymbol, from, to));
    }
}