package org.example.binancetracker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity handleBadRequest(InvalidDateRangeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BinanceApiException.class)
    public ResponseEntity handleBinanceError(BinanceApiException ex) {
        log.error("Binance API error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "Binance integration failure: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleGeneral(Exception ex) {
        log.error("Unhandled server error: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred");
    }

    private ResponseEntity buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}