package org.example.binancetracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "crypto_prices",
        indexes = {
                @Index(name = "idx_crypto_prices_symbol_fetched_at", columnList = "symbol, fetched_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}