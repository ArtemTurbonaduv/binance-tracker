package org.example.binancetracker.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.example.binancetracker.dto.AveragePriceResponse;
import org.example.binancetracker.dto.PageResponse;
import org.example.binancetracker.dto.PriceDto;
import org.example.binancetracker.dto.TrendResponse;
import org.example.binancetracker.service.PriceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PriceControllerIntegrationTest {

    static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceService priceService;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Имитация внешнего API Binance через WireMock
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"mins\": 5, \"price\": \"705.50000000\", \"closeTime\": 1699999999}")));
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("GET /api/v1/prices/latest - успешный возврат последней цены")
    void getLatestPrice_Success() throws Exception {
        PriceDto dto = new PriceDto("BNBUSDT", new BigDecimal("705.50000000"), Instant.parse("2026-11-03T10:00:00Z"));
        when(priceService.getLatestPrice("BNBUSDT")).thenReturn(dto);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/prices/latest")
                        .param("symbol", "BNBUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.price").value(705.5));
    }

    @Test
    @DisplayName("GET /api/v1/prices/trend - корректный расчет тренда")
    void getTrend_Success() throws Exception {
        Instant from = Instant.parse("2026-11-03T09:00:00Z");
        Instant to = Instant.parse("2026-11-03T10:00:00Z");
        TrendResponse trend = new TrendResponse("BNBUSDT", from, to, "UPWARD");

        when(priceService.calculateTrend("BNBUSDT", from, to)).thenReturn(trend);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/prices/trend")
                        .param("symbol", "BNBUSDT")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("UPWARD"));
    }

    @Test
    @DisplayName("GET /api/v1/prices/average - расчет средней цены")
    void getAverage_Success() throws Exception {
        Instant from = Instant.parse("2026-11-03T09:00:00Z");
        Instant to = Instant.parse("2026-11-03T10:00:00Z");
        AveragePriceResponse avg = new AveragePriceResponse(new BigDecimal("702.15000000"));

        when(priceService.getAveragePrice("BNBUSDT", from, to)).thenReturn(avg);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/prices/average")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePrice").value(702.15));
    }

    @Test
    @DisplayName("GET /api/v1/prices - постраничная выдача")
    void getPrices_Success() throws Exception {
        Instant from = Instant.parse("2026-11-03T09:00:00Z");
        Instant to = Instant.parse("2026-11-03T10:00:00Z");
        PriceDto item = new PriceDto("BNBUSDT", new BigDecimal("700.00"), from);
        PageResponse<PriceDto> page = new PageResponse<>(List.of(item), 0, 10, 1L, false);

        when(priceService.getPrices(eq("BNBUSDT"), eq(from), eq(to), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/prices")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}