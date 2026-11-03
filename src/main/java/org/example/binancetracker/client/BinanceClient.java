package org.example.binancetracker.client;

import org.example.binancetracker.client.dto.BinanceAvgPriceResponse;
import org.example.binancetracker.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);
    private final RestClient restClient;

    public BinanceClient(RestClient binanceRestClient) {
        this.restClient = binanceRestClient;
    }

    public BinanceAvgPriceResponse fetchAvgPrice(String symbol) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/avgPrice")
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int code = response.getStatusCode().value();
                        log.error("Binance API returned error status: {}", code);
                        throw new BinanceApiException("Binance API error response: " + code);
                    })
                    .body(BinanceAvgPriceResponse.class);
        } catch (BinanceApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Network or serialization error while fetching price from Binance: {}", e.getMessage());
            throw new BinanceApiException("Failed to communicate with Binance API", e);
        }
    }
}