package org.example.binancetracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class BinanceClientConfig {

    @Value("${binance.api.base-url:https://api.binance.com}")
    private String baseUrl;

    @Value("${binance.api.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${binance.api.read-timeout-ms:5000}")
    private int readTimeout;

    @Bean
    public RestClient binanceRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}