package org.example.binancetracker.controller;

import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.example.binancetracker.service.PriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;
    private final String defaultSymbol;

    public PriceController(
            PriceService priceService,
            @Value("${binance.default-symbol:BNBUSDT}") String defaultSymbol) {
        this.priceService = priceService;
        this.defaultSymbol = defaultSymbol;
    }

    // 1. Актуальная последняя цена
    @GetMapping("/latest")
    public ResponseEntity<PriceDto> getLatestPrice(
            @RequestParam(required = false) String symbol) {
        String targetSymbol = resolveSymbol(symbol);
        return ResponseEntity.ok(priceService.getLatestPrice(targetSymbol));
    }

    // 2. Средняя цена за интервал [from, to]
    @GetMapping("/average")
    public ResponseEntity<AveragePriceResponse> getAveragePrice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String symbol) {
        String targetSymbol = resolveSymbol(symbol);
        return ResponseEntity.ok(priceService.getAveragePrice(targetSymbol, from, to));
    }

    // 3. Записи за интервал с пагинацией
    @GetMapping
    public ResponseEntity<PageResponse<PriceDto>> getPricesHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String symbol) {
        String targetSymbol = resolveSymbol(symbol);
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(priceService.getPrices(targetSymbol, from, to, pageRequest));
    }

    // 4. Тренд за интервал
    @GetMapping("/trend")
    public ResponseEntity<TrendResponse> getTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String symbol) {
        String targetSymbol = resolveSymbol(symbol);
        return ResponseEntity.ok(priceService.calculateTrend(targetSymbol, from, to));
    }

    private String resolveSymbol(String symbol) {
        return (symbol != null && !symbol.isBlank()) ? symbol.toUpperCase() : defaultSymbol;
    }
}