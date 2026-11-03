package org.example.binancetracker.scheduler;

import org.example.binancetracker.client.BinanceClient;
import org.example.binancetracker.client.dto.BinanceAvgPriceResponse;
import org.example.binancetracker.entity.CryptoPrice;
import org.example.binancetracker.repository.CryptoPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BinancePricePoller {

    private static final Logger log = LoggerFactory.getLogger(BinancePricePoller.class);

    private final BinanceClient binanceClient;
    private final CryptoPriceRepository repository;
    private final String defaultSymbol;

    public BinancePricePoller(
            BinanceClient binanceClient,
            CryptoPriceRepository repository,
            @Value("${binance.default-symbol:BNBUSDT}") String defaultSymbol) {
        this.binanceClient = binanceClient;
        this.repository = repository;
        this.defaultSymbol = defaultSymbol;
    }

    @Scheduled(fixedDelayString = "${binance.poll.interval:PT30S}")
    public void pollPrice() {
        try {
            log.debug("Polling Binance for symbol: {}", defaultSymbol);
            BinanceAvgPriceResponse response = binanceClient.fetchAvgPrice(defaultSymbol);

            if (response != null && response.price() != null) {
                CryptoPrice priceEntity = CryptoPrice.builder()
                        .symbol(defaultSymbol)
                        .price(response.price())
                        .fetchedAt(Instant.now())
                        .build();

                repository.save(priceEntity);
                log.info("Saved price {} for symbol {} at {}", priceEntity.getPrice(), defaultSymbol, priceEntity.getFetchedAt());
            }
        } catch (Exception ex) {
            // Предотвращает падение шедуллера при сетевых ошибках, Rate Limit или сбое десериализации
            log.error("Failed to poll Binance price for {}: {}", defaultSymbol, ex.getMessage());
        }
    }
}