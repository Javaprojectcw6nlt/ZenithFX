package com.zenithfx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
public class OandaService {
    @Value("${oanda.base-url:https://api-fxpractice.oanda.com}")
    private String baseUrl;
    @Value("${oanda.account-id:}")
    private String accountId;
    @Value("${oanda.api-token:}")
    private String apiToken;

    private final ObjectMapper mapper = new ObjectMapper();

    private RestClient getClient() {
        if (apiToken == null || apiToken.isBlank() || accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("OANDA credentials are missing in application.properties.");
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String cleanId() {
        return accountId.replaceAll("\\s+", "");
    }

    public JsonNode accountSummary() {
        try {
            String body = getClient().get()
                    .uri("/v3/accounts/" + cleanId() + "/summary")
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    public JsonNode pricing(String instrument) {
        try {
            String safeInstrument = normalizeInstrument(instrument);
            String body = getClient().get()
                    .uri("/v3/accounts/" + cleanId() + "/pricing?instruments=" + safeInstrument)
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    public JsonNode candles(String instrument, String granularity) {
        try {
            String safeInstrument = normalizeInstrument(instrument);
            // FIX: a granularity is one letter followed by up to two digits (D, W, M, H1, H4, M5, M15 ...).
            // The old pattern "[A-Z]{1,2}" rejected every value containing a digit and silently used "D".
            String upperGranularity = granularity == null ? "" : granularity.trim().toUpperCase();
            String safeGranularity = upperGranularity.matches("[A-Z][0-9]{0,2}") ? upperGranularity : "D";
            String body = getClient().get()
                    .uri("/v3/instruments/" + safeInstrument + "/candles?count=10&granularity=" + safeGranularity + "&price=M")
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    public JsonNode positions() {
        try {
            String body = getClient().get()
                    .uri("/v3/accounts/" + cleanId() + "/openPositions")
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    public JsonNode openMarketOrder(String instrument, long units) {
        if (units == 0) throw new IllegalArgumentException("Quantity must not be zero.");
        try {
            String safeInstrument = normalizeInstrument(instrument);
            Map<String, Object> orderData = new HashMap<>();
            Map<String, Object> order = new HashMap<>();
            order.put("type", "MARKET");
            order.put("instrument", safeInstrument);
            order.put("units", String.valueOf(units));
            order.put("timeInForce", "FOK");
            order.put("positionFill", "DEFAULT");
            orderData.put("order", order);

            String body = getClient().post()
                    .uri("/v3/accounts/" + cleanId() + "/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(orderData)
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    public JsonNode closeTrade(long tradeId) {
        try {
            Map<String, Object> closeData = new HashMap<>();
            closeData.put("units", "ALL");

            String body = getClient().put()
                    .uri("/v3/accounts/" + cleanId() + "/trades/" + tradeId + "/close")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(closeData)
                    .retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException("OANDA connection error: " + e.getMessage());
        }
    }

    private String normalizeInstrument(String instrument) {
        String safe = instrument == null ? "EUR_USD" : instrument.toUpperCase().replace('/', '_').replaceAll("[^A-Z_]", "");
        if (!safe.matches("[A-Z]{3}_[A-Z]{3}")) throw new IllegalArgumentException("Choose a valid instrument such as EUR_USD.");
        return safe;
    }
}