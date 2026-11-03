package org.example.binancetracker.service.impl;

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

@Service
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private static final BigDecimal TREND_THRESHOLD_PERCENT = new BigDecimal("0.1"); // 0.1% порог FLAT

    private final CryptoPriceRepository repository;

    public PriceServiceImpl(CryptoPriceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PriceDto getLatestPrice(String symbol) {
        return repository.findFirstBySymbolOrderByFetchedAtDesc(symbol)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No price records found for symbol: " + symbol));
    }

    @Override
    public AveragePriceResponse getAveragePrice(String symbol, Instant from, Instant to) {
        validateDateRange(from, to);

        BigDecimal avg = repository.findAveragePrice(symbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException("No price data in range [" + from + ", " + to + "]"));

        return new AveragePriceResponse(avg.setScale(8, RoundingMode.HALF_UP));
    }

    @Override
    public PageResponse<PriceDto> getPrices(String symbol, Instant from, Instant to, Pageable pageable) {
        validateDateRange(from, to);

        Page<CryptoPrice> page = repository.findAllBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(
                symbol, from, to, pageable
        );

        List<PriceDto> items = page.getContent().stream()
                .map(this::toDto)
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
        validateDateRange(from, to);

        CryptoPrice firstRecord = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException("No data to calculate trend for symbol: " + symbol));

        CryptoPrice lastRecord = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, from, to)
                .orElseThrow(() -> new ResourceNotFoundException("No data to calculate trend for symbol: " + symbol));

        BigDecimal startPrice = firstRecord.getPrice();
        BigDecimal endPrice = lastRecord.getPrice();

        // Формула: ((endPrice - startPrice) / startPrice) * 100
        BigDecimal delta = endPrice.subtract(startPrice);
        BigDecimal percentChange = delta
                .divide(startPrice, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        String trend;
        if (percentChange.abs().compareTo(TREND_THRESHOLD_PERCENT) < 0) {
            trend = "FLAT";
        } else if (percentChange.compareTo(BigDecimal.ZERO) > 0) {
            trend = "UPWARD";
        } else {
            trend = "DOWNWARD";
        }

        return new TrendResponse(symbol, from, to, trend);
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Parameters 'from' and 'to' must not be null");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' date must be before or equal to 'to' date");
        }
    }

    private PriceDto toDto(CryptoPrice entity) {
        return new PriceDto(entity.getSymbol(), entity.getPrice(), entity.getFetchedAt());
    }
}