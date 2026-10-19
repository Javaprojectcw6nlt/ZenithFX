package com.zenithfx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Service
public class OandaService {
    @Value("${oanda.base-url:https://api-fxpractice.oanda.com}")
    private String baseUrl;
    @Value("${oanda.account-id:}")
    private String accountId;
    @Value("${oanda.api-token:}")
    private String apiToken;

    private final ObjectMapper mapper = new ObjectMapper();

    private RestClient client() {
        if (apiToken == null || apiToken.isBlank() || accountId == null || accountId.isBlank()
            || accountId.contains("PASTE_YOUR")) {
            throw new IllegalStateException("OANDA is not configured. Copy application-local.example.properties to application-local.properties and add your PRACTICE account ID and API token.");
        }
        return RestClient.builder().baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public JsonNode accountSummary() {
        return get("/v3/accounts/" + accountId + "/summary");
    }

    public JsonNode pricing(String instrument) {
        return get("/v3/accounts/" + accountId + "/pricing?instruments=" + encodeInstrument(instrument));
    }

    public JsonNode candles(String instrument, String granularity) {
        String safeGranularity = (granularity == null || !granularity.matches("[A-Z]{1,2}")) ? "D" : granularity;
        return get("/v3/instruments/" + encodeInstrument(instrument) + "/candles?count=10&granularity=" + safeGranularity + "&price=M");
    }

    public JsonNode positions() {
        return get("/v3/accounts/" + accountId + "/openPositions");
    }

    public JsonNode openMarketOrder(String instrument, long units) {
        if (units == 0) throw new IllegalArgumentException("Quantity must not be zero. Positive units open long; negative units open short.");
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("order", Map.of(
            "type", "MARKET",
            "instrument", normalizeInstrument(instrument),
            "units", Long.toString(units),
            "timeInForce", "FOK",
            "positionFill", "DEFAULT"
        ));
        return post("/v3/accounts/" + accountId + "/orders", order);
    }

    public JsonNode closeTrade(long tradeId) {
        return put("/v3/accounts/" + accountId + "/trades/" + tradeId + "/close", Map.of("units", "ALL"));
    }

    private JsonNode get(String path) {
        try {
            String body = client().get().uri(path).retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private JsonNode post(String path, Object payload) {
        try {
            String body = client().post().uri(path).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private JsonNode put(String path, Object payload) {
        try {
            String body = client().put().uri(path).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(String.class);
            return mapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OANDA API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private String encodeInstrument(String instrument) {
        return normalizeInstrument(instrument);
    }

    private String normalizeInstrument(String instrument) {
        String safe = instrument == null ? "EUR_USD" : instrument.toUpperCase(Locale.ROOT).replace('/', '_').replaceAll("[^A-Z_]", "");
        if (!safe.matches("[A-Z]{3}_[A-Z]{3}")) throw new IllegalArgumentException("Choose a valid instrument such as EUR_USD.");
        return safe;
    }
}
