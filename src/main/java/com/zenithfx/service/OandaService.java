package com.zenithfx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.pricing.ClientPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private Context getContext() {
        if (apiToken == null || apiToken.isBlank() || accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("OANDA credentials are missing in application.properties.");
        }
        return new Context(baseUrl, apiToken);
    }

    // Helper to clean up instrument names
    private String normalizeInstrument(String instrument) {
        String safe = instrument == null ? "EUR_USD" : instrument.toUpperCase(Locale.ROOT).replace('/', '_').replaceAll("[^A-Z_]", "");
        if (!safe.matches("[A-Z]{3}_[A-Z]{3}")) throw new IllegalArgumentException("Choose a valid instrument such as EUR_USD.");
        return safe;
    }

    // Helper to handle the SDK's null error bug on Java 25
    private IllegalStateException handleSdkError(Exception e, String action) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return new IllegalStateException("OANDA SDK Error (" + action + "): The OANDA server rejected the request. This usually means the Account ID is in the wrong format (e.g., missing dashes like 101-004-1234567-001) or the API token is invalid.");
        }
        return new IllegalStateException("OANDA SDK Error (" + action + "): " + msg);
    }

    public JsonNode accountSummary() {
        try {
            Context ctx = getContext();
            String cleanId = accountId.replaceAll("\\s+", "");
            AccountSummary summary = ctx.account.summary(new AccountID(cleanId)).getAccount();
            return mapper.valueToTree(summary);
        } catch (Exception e) {
            throw handleSdkError(e, "Account");
        }
    }

    public JsonNode pricing(String instrument) {
        try {
            Context ctx = getContext();
            String cleanId = accountId.replaceAll("\\s+", "");
            String safeInstrument = normalizeInstrument(instrument);
            List<ClientPrice> prices = ctx.pricing.get(new AccountID(cleanId), new InstrumentName(safeInstrument)).getPrices();
            Map<String, Object> response = new HashMap<>();
            response.put("prices", prices);
            return mapper.valueToTree(response);
        } catch (Exception e) {
            throw handleSdkError(e, "Pricing");
        }
    }

    public JsonNode candles(String instrument, String granularity) {
        try {
            Context ctx = getContext();
            String safeGranularity = (granularity == null || !granularity.matches("[A-Z]{1,2}")) ? "D" : granularity;
            String safeInstrument = normalizeInstrument(instrument);

            InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(safeInstrument));
            request.setGranularity(com.oanda.v20.instrument.CandlestickGranularity.valueOf(safeGranularity));
            request.setCount(10);
            request.setPrice(com.oanda.v20.instrument.CandlestickPrice.Types.MID);

            List<Candlestick> candleList = ctx.instrument.candles(request).getCandles();
            Map<String, Object> response = new HashMap<>();
            response.put("candles", candleList);
            return mapper.valueToTree(response);
        } catch (Exception e) {
            throw handleSdkError(e, "Candles");
        }
    }

    public JsonNode positions() {
        try {
            Context ctx = getContext();
            String cleanId = accountId.replaceAll("\\s+", "");
            var positions = ctx.position.listOpen(new AccountID(cleanId)).getPositions();
            Map<String, Object> response = new HashMap<>();
            response.put("positions", positions);
            return mapper.valueToTree(response);
        } catch (Exception e) {
            throw handleSdkError(e, "Positions");
        }
    }

    public JsonNode openMarketOrder(String instrument, long units) {
        if (units == 0) throw new IllegalArgumentException("Quantity must not be zero.");
        try {
            Context ctx = getContext();
            String cleanId = accountId.replaceAll("\\s+", "");
            String safeInstrument = normalizeInstrument(instrument);

            com.oanda.v20.order.MarketOrderRequest orderRequest = new com.oanda.v20.order.MarketOrderRequest();
            orderRequest.setInstrument(new InstrumentName(safeInstrument));
            orderRequest.setUnits(new com.oanda.v20.primitives.DecimalNumber(units));
            orderRequest.setTimeInForce(com.oanda.v20.order.OrderTimeInForce.FOK);

            var transaction = ctx.order.create(new AccountID(cleanId), orderRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("orderCreateTransaction", transaction.getOrderCreateTransaction());
            return mapper.valueToTree(response);
        } catch (Exception e) {
            throw handleSdkError(e, "Open Order");
        }
    }

    public JsonNode closeTrade(long tradeId) {
        try {
            Context ctx = getContext();
            String cleanId = accountId.replaceAll("\\s+", "");
            com.oanda.v20.trade.TradeCloseRequest request = new com.oanda.v20.trade.TradeCloseRequest(new com.oanda.v20.trade.TradeID(String.valueOf(tradeId)));
            request.setUnits(com.oanda.v20.trade.TradeCloseRequest.Units.ALL);

            var transaction = ctx.trade.close(new AccountID(cleanId), request);
            Map<String, Object> response = new HashMap<>();
            response.put("closeTransaction", transaction.getOrderCreateTransaction());
            return mapper.valueToTree(response);
        } catch (Exception e) {
            throw handleSdkError(e, "Close Trade");
        }
    }
}