CREATE TABLE crypto_prices (
                               id BIGSERIAL PRIMARY KEY,
                               symbol VARCHAR(20) NOT NULL,
                               price NUMERIC(18, 8) NOT NULL,
                               fetched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_crypto_prices_symbol_fetched_at ON crypto_prices(symbol, fetched_at);