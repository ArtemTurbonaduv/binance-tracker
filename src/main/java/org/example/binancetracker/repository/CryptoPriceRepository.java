package org.example.binancetracker.repository;

import org.example.binancetracker.entity.CryptoPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {

    // 1. Поиск последней сохраненной записи
    Optional<CryptoPrice> findFirstBySymbolOrderByFetchedAtDesc(String symbol);

    // 2. Расчет средней цены за период [from, to] в базе данных
    @Query("SELECT AVG(p.price) FROM CryptoPrice p WHERE p.symbol = :symbol AND p.fetchedAt >= :from AND p.fetchedAt <= :to")
    Optional<BigDecimal> findAveragePrice(
            @Param("symbol") String symbol,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // 3. Получение записей с пагинацией за период [from, to]
    Page<CryptoPrice> findAllBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(
            String symbol,
            Instant from,
            Instant to,
            Pageable pageable
    );

    // 4. Вспомогательные методы для тренда: первая и последняя запись в интервале
    Optional<CryptoPrice> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(
            String symbol,
            Instant from,
            Instant to
    );

    Optional<CryptoPrice> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(
            String symbol,
            Instant from,
            Instant to
    );
}