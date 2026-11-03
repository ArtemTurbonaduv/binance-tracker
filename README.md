# Binance Crypto Price Tracker

Spring Boot 3 REST-сервис для периодического мониторинга цен криптовалют с биржи Binance, сохранения их в PostgreSQL и предоставления аналитики (актуальная цена, средняя цена за период, пагинированная история и определение тренда).

---

## Технологический стек

- **Java:** 17
- **Фреймворк:** Spring Boot 3.3.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`)
- **База данных:** PostgreSQL 16
- **Миграции БД:** Flyway
- **HTTP-клиент:** Spring `RestClient`
- **Контейнеризация:** Docker, Docker Compose
- **Тестирование:** JUnit 5, Mockito, Testcontainers, WireMock

---

## Быстрый старт

### 1. Запуск через Docker Compose (рекомендуемый способ)

Для сборки и запуска всего стека (PostgreSQL + Spring Boot сервис) выполните в корне проекта:

```bash
docker compose up --build -d
```

После старта:
- Сервис доступен по адресу: `http://localhost:8080`
- PostgreSQL доступен на порту: `5432`

Для остановки контейнеров:
```bash
docker compose down
```

---

### 2. Локальный запуск для разработки

1. Запустите только базу данных через Docker:
   ```bash
   docker compose up -d postgres
   ```
2. Запустите Spring Boot приложение через Maven Wrapper:
   ```bash
   ./mvnw clean spring-boot:run
   ```
   *(На Windows: `.\mvnw.cmd clean spring-boot:run`)*

---

## Конфигурация (`application.yml`)

Основные параметры сервиса настраиваются через переменные окружения:

| Переменная окружения | Описание | Значение по умолчанию |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC URL базы данных | `jdbc:postgresql://localhost:5432/cryptodb` |
| `SPRING_DATASOURCE_USERNAME` | Имя пользователя БД | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль к БД | `postgres` |
| `BINANCE_POLL_INTERVAL` | Интервал опроса биржи Binance | `PT30S` (каждые 30 секунд) |
| `binance.default-symbol` | Торговая пара по умолчанию | `BNBUSDT` |
| `binance.api.base-url` | Базовый URL API Binance | `https://api.binance.com` |

---

## Описание REST API

Все временные метки (`from`, `to`, `fetchedAt`) передаются и возвращаются в формате **ISO-8601 UTC** (`YYYY-MM-DDTHH:mm:ssZ`).

### 1. Получение последней цены
Возвращает самую свежую сохраненную цену по указанному символу (или паре по умолчанию `BNBUSDT`).

- **Метод:** `GET /api/v1/prices/latest`
- **Параметры:**
    - `symbol` *(опционально)*: тикер торговой пары (например, `BNBUSDT`, `BTCUSDT`).

```bash
curl -s http://localhost:8080/api/v1/prices/latest
```

**Пример ответа (200 OK):**
```json
{
  "symbol": "BNBUSDT",
  "price": 701.02241147,
  "fetchedAt": "2026-11-03T00:27:41.301744600Z"
}
```

---

### 2. Средняя цена за период
Рассчитывает среднее арифметическое значение цены за интервал времени `[from, to]` средствами базы данных.

- **Метод:** `GET /api/v1/prices/average`
- **Параметры:**
    - `from` *(обязательно)*: начало периода в ISO-8601 UTC.
    - `to` *(обязательно)*: конец периода в ISO-8601 UTC.
    - `symbol` *(опционально)*: тикер торговой пары.

```bash
curl -s "http://localhost:8080/api/v1/prices/average?from=2026-11-01T00:00:00Z&to=2026-11-05T00:00:00Z&symbol=BNBUSDT"
```

**Пример ответа (200 OK):**
```json
{
  "averagePrice": 701.03417569
}
```

---

### 3. История цен с пагинацией
Возвращает список сохраненных цен за интервал `[from, to]` с сортировкой от самых свежих к старым.

- **Метод:** `GET /api/v1/prices`
- **Параметры:**
    - `from` *(обязательно)*: начало периода.
    - `to` *(обязательно)*: конец периода.
    - `page` *(опционально, default: 0)*: номер страницы (начиная с 0).
    - `size` *(опционально, default: 20)*: количество записей на страницу.
    - `symbol` *(опционально)*: тикер торговой пары.

```bash
curl -s "http://localhost:8080/api/v1/prices?from=2026-11-01T00:00:00Z&to=2026-11-05T00:00:00Z&page=0&size=10"
```

**Пример ответа (200 OK):**
```json
{
  "items": [
    {
      "symbol": "BNBUSDT",
      "price": 701.07364329,
      "fetchedAt": "2026-11-03T00:28:11.715782Z"
    },
    {
      "symbol": "BNBUSDT",
      "price": 701.02241147,
      "fetchedAt": "2026-11-03T00:27:41.301744Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "hasNext": false
}
```

---

### 4. Определение тренда цены
Сравнивает первую и последнюю сохраненную цену за выбранный временной интервал:
- `UPWARD`: прирост цены > 0.1%
- `DOWNWARD`: снижение цены > 0.1%
- `FLAT`: абсолютное изменение $\le$ 0.1%

- **Метод:** `GET /api/v1/prices/trend`
- **Параметры:**
    - `from` *(обязательно)*: начало периода.
    - `to` *(обязательно)*: конец периода.
    - `symbol` *(опционально)*: тикер торговой пары.

```bash
curl -s "http://localhost:8080/api/v1/prices/trend?from=2026-11-01T00:00:00Z&to=2026-11-05T00:00:00Z"
```

**Пример ответа (200 OK):**
```json
{
  "symbol": "BNBUSDT",
  "start_date": "2026-11-01T00:00:00Z",
  "end_date": "2026-11-05T00:00:00Z",
  "trend": "UPWARD"
}
```

---

## Запуск тестов

Для запуска полного набора юнит- и интеграционных тестов (с использованием Testcontainers для PostgreSQL и WireMock для внешнего API Binance):

```bash
./mvnw clean test
```