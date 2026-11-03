package org.example.binancetracker.service;

import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.example.binancetracker.entity.CryptoPrice;
import org.example.binancetracker.exception.InvalidDateRangeException;
import org.example.binancetracker.exception.ResourceNotFoundException;
import org.example.binancetracker.repository.CryptoPriceRepository;
import org.example.binancetracker.service.impl.PriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

    @Mock
    private CryptoPriceRepository repository;

    @InjectMocks
    private PriceServiceImpl priceService;

    private final String symbol = "BNBUSDT";
    private final Instant now = Instant.parse("2026-11-03T10:00:00Z");
    private final Instant hourAgo = now.minusSeconds(3600);

    @Test
    @DisplayName("getLatestPrice возвращает последнюю цену")
    void getLatestPrice_Success() {
        CryptoPrice entity = CryptoPrice.builder()
                .id(1L)
                .symbol(symbol)
                .price(new BigDecimal("700.50000000"))
                .fetchedAt(now)
                .build();

        when(repository.findFirstBySymbolOrderByFetchedAtDesc(symbol))
                .thenReturn(Optional.of(entity));

        PriceDto result = priceService.getLatestPrice(symbol);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo(symbol);
        assertThat(result.price()).isEqualByComparingTo("700.50000000");
    }

    @Test
    @DisplayName("getLatestPrice выбрасывает 404 если данных нет")
    void getLatestPrice_NotFound() {
        when(repository.findFirstBySymbolOrderByFetchedAtDesc(symbol))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.getLatestPrice(symbol))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAveragePrice корректно возвращает среднюю цену")
    void getAveragePrice_Success() {
        when(repository.findAveragePrice(symbol, hourAgo, now))
                .thenReturn(Optional.of(new BigDecimal("650.12345678")));

        AveragePriceResponse response = priceService.getAveragePrice(symbol, hourAgo, now);

        assertThat(response.averagePrice()).isEqualByComparingTo("650.12345678");
    }

    @Test
    @DisplayName("Валидация диапазона дат: from после to выбрасывает InvalidDateRangeException")
    void validateDateRange_FromAfterTo() {
        assertThatThrownBy(() -> priceService.getAveragePrice(symbol, now, hourAgo))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    @DisplayName("getPrices возвращает корректную пагинацию")
    void getPrices_Pagination() {
        Pageable pageable = PageRequest.of(0, 10);
        CryptoPrice entity = CryptoPrice.builder()
                .id(1L)
                .symbol(symbol)
                .price(new BigDecimal("700.00000000"))
                .fetchedAt(now)
                .build();

        when(repository.findAllBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, hourAgo, now, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResponse<PriceDto> response = priceService.getPrices(symbol, hourAgo, now, pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("calculateTrend определяет UPWARD тренд")
    void calculateTrend_Upward() {
        CryptoPrice start = CryptoPrice.builder().price(new BigDecimal("100.00")).build();
        CryptoPrice end = CryptoPrice.builder().price(new BigDecimal("105.00")).build(); // +5%

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, hourAgo, now))
                .thenReturn(Optional.of(start));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, hourAgo, now))
                .thenReturn(Optional.of(end));

        TrendResponse trend = priceService.calculateTrend(symbol, hourAgo, now);

        assertThat(trend.trend()).isEqualTo("UPWARD");
    }

    @Test
    @DisplayName("calculateTrend определяет DOWNWARD тренд")
    void calculateTrend_Downward() {
        CryptoPrice start = CryptoPrice.builder().price(new BigDecimal("100.00")).build();
        CryptoPrice end = CryptoPrice.builder().price(new BigDecimal("95.00")).build(); // -5%

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, hourAgo, now))
                .thenReturn(Optional.of(start));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, hourAgo, now))
                .thenReturn(Optional.of(end));

        TrendResponse trend = priceService.calculateTrend(symbol, hourAgo, now);

        assertThat(trend.trend()).isEqualTo("DOWNWARD");
    }

    @Test
    @DisplayName("calculateTrend определяет FLAT тренд при изменении менее 0.1%")
    void calculateTrend_Flat() {
        CryptoPrice start = CryptoPrice.builder().price(new BigDecimal("100.000")).build();
        CryptoPrice end = CryptoPrice.builder().price(new BigDecimal("100.050")).build(); // +0.05% (< 0.1%)

        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, hourAgo, now))
                .thenReturn(Optional.of(start));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, hourAgo, now))
                .thenReturn(Optional.of(end));

        TrendResponse trend = priceService.calculateTrend(symbol, hourAgo, now);

        assertThat(trend.trend()).isEqualTo("FLAT");
    }
}