package org.example.binancetracker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.example.binancetracker.entity.CryptoPrice;
import org.example.binancetracker.exception.InvalidDateRangeException;
import org.example.binancetracker.exception.ResourceNotFoundException;
import org.example.binancetracker.repository.CryptoPriceRepository;
import org.example.binancetracker.service.PriceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private final CryptoPriceRepository repository;

    private static final Instant MIN_SUPPORTED_INSTANT = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant MAX_SUPPORTED_INSTANT = Instant.parse("2099-12-31T23:59:59Z");
    private static final BigDecimal FLAT_THRESHOLD_PERCENT = new BigDecimal("0.001"); // 0.1%

    @Override
    public PriceDto getLatestPrice(String symbol) {
        String sanitizedSymbol = sanitizeSymbol(symbol);
        CryptoPrice price = repository.findFirstBySymbolOrderByFetchedAtDesc(sanitizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("No price records found for symbol: " + sanitizedSymbol));
        return new PriceDto(price.getSymbol(), price.getPrice(), price.getFetchedAt());
    }

    @Override
    public AveragePriceResponse getAveragePrice(String symbol, Instant from, Instant to) {
        String sanitizedSymbol = sanitizeSymbol(symbol);
        validateDateRange(from, to);

        BigDecimal average = repository.findAveragePrice(sanitizedSymbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No prices found for symbol '%s' in the given period", sanitizedSymbol)));

        return new AveragePriceResponse(average.setScale(8, RoundingMode.HALF_UP));
    }

    @Override
    public PageResponse<PriceDto> getPrices(String symbol, Instant from, Instant to, Pageable pageable) {
        String sanitizedSymbol = sanitizeSymbol(symbol);
        validateDateRange(from, to);

        Page<CryptoPrice> page = repository.findAllBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(
                sanitizedSymbol, from, to, pageable);

        List<PriceDto> items = page.getContent().stream()
                .map(p -> new PriceDto(p.getSymbol(), p.getPrice(), p.getFetchedAt()))
                .toList();

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext()
        );
    }

    @Override
    public TrendResponse calculateTrend(String symbol, Instant from, Instant to) {
        String sanitizedSymbol = sanitizeSymbol(symbol);
        validateDateRange(from, to);

        CryptoPrice startPrice = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(sanitizedSymbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException("No data available to calculate trend for the period"));

        CryptoPrice endPrice = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(sanitizedSymbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException("No data available to calculate trend for the period"));

        String trend = determineTrend(startPrice.getPrice(), endPrice.getPrice());
        return new TrendResponse(sanitizedSymbol, from, to, trend);
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Parameters 'from' and 'to' must not be null");
        }
        if (from.isBefore(MIN_SUPPORTED_INSTANT) || to.isAfter(MAX_SUPPORTED_INSTANT)) {
            throw new InvalidDateRangeException(String.format(
                    "Dates must be between %s and %s", MIN_SUPPORTED_INSTANT, MAX_SUPPORTED_INSTANT));
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("Parameter 'from' must be strictly before or equal to 'to'");
        }
    }

    private String sanitizeSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return "BNBUSDT";
        }
        return symbol.trim().toUpperCase();
    }

    private String determineTrend(BigDecimal start, BigDecimal end) {
        if (start.compareTo(BigDecimal.ZERO) == 0) {
            return "FLAT";
        }

        BigDecimal difference = end.subtract(start);
        BigDecimal relativeChange = difference.abs().divide(start, 8, RoundingMode.HALF_UP);

        if (relativeChange.compareTo(FLAT_THRESHOLD_PERCENT) < 0) {
            return "FLAT";
        }

        return difference.compareTo(BigDecimal.ZERO) > 0 ? "UPWARD" : "DOWNWARD";
    }
}