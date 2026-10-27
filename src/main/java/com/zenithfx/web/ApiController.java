package com.zenithfx.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenithfx.service.HnbSoapService;
import com.zenithfx.service.OandaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final HnbSoapService hnb;
    private final OandaService oanda;

    public ApiController(HnbSoapService hnb, OandaService oanda) {
        this.hnb = hnb;
        this.oanda = oanda;
    }

    // --- SOAP Endpoint ---
    @GetMapping("/hnb")
    public Map<String, Object> hnb(
        @RequestParam(defaultValue = "EUR") String currency,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        List<Map<String, String>> rates = hnb.getRates(currency, startDate, endDate);
        return Map.of("currency", currency, "startDate", startDate, "endDate", endDate, "rates", rates);
    }

    // --- OANDA Endpoints (Now powered by the SDK) ---
    @GetMapping("/oanda/account")
    public JsonNode account() { return oanda.accountSummary(); }

    @GetMapping("/oanda/pricing")
    public JsonNode pricing(@RequestParam(defaultValue = "EUR_USD") String instrument) { 
        return oanda.pricing(instrument); 
    }

    @GetMapping("/oanda/candles")
    public JsonNode candles(@RequestParam(defaultValue = "EUR_USD") String instrument,
                            @RequestParam(defaultValue = "D") String granularity) {
        return oanda.candles(instrument, granularity);
    }

    @GetMapping("/oanda/positions")
    public JsonNode positions() { return oanda.positions(); }

    @PostMapping("/oanda/orders")
    public JsonNode openOrder(@RequestParam String instrument, @RequestParam long units) {
        return oanda.openMarketOrder(instrument, units);
    }

    @PostMapping("/oanda/trades/{tradeId}/close")
    public JsonNode closeTrade(@PathVariable long tradeId) { return oanda.closeTrade(tradeId); }

    // --- Bulletproof Error Handling ---
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleKnownErrors(RuntimeException ex) {
        return Map.of("error", ex.getMessage() == null ? "Request failed." : ex.getMessage());
    }
}