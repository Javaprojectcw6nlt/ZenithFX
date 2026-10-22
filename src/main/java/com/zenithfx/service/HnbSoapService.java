
package com.zenithfx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class HnbSoapService {

    private static final Logger log =
            LoggerFactory.getLogger(HnbSoapService.class);

    private static final String SOAP_NAMESPACE =
            "http://www.mnb.hu/webservices/";

    private static final String SOAP_ACTION =
            "http://tempuri.org/GetExchangeRates";

    private static final List<String> ENDPOINTS = List.of(
            "https://www.mnb.hu/arfolyamok.asmx",
            "http://www.mnb.hu/arfolyamok.asmx"
    );

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public List<Map<String, String>> getRates(
            String currency,
            LocalDate startDate,
            LocalDate endDate) throws Exception {

        String requestId = UUID.randomUUID().toString();

        validate(startDate, endDate);

        String currencies = normalizeCurrency(currency);

        String envelope = buildEnvelope(
                currencies, startDate, endDate
        );

        log.info(
                "[MNB:{}] Request started: currencies={}, start={}, end={}",
                requestId, currencies, startDate, endDate
        );

        List<String> failures = new ArrayList<>();

        for (int i = 0; i < ENDPOINTS.size(); i++) {
            String endpoint = ENDPOINTS.get(i);

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(TIMEOUT)
                        .header(
                                "Content-Type",
                                "text/xml; charset=utf-8"
                        )
                        .header("Accept", "text/xml")
                        .header(
                                "SOAPAction",
                                "\"" + SOAP_ACTION + "\""
                        )
                        .POST(HttpRequest.BodyPublishers.ofString(
                                envelope, StandardCharsets.UTF_8
                        ))
                        .build();

                log.info(
                        "[MNB:{}] POST {} (endpoint {}/{})",
                        requestId, endpoint,
                        i + 1, ENDPOINTS.size()
                );

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

                int status = response.statusCode();
                String body = response.body() == null
                        ? "" : response.body();

                log.info(
                        "[MNB:{}] HTTP status={}, responseLength={}, " +
                        "finalURL={}",
                        requestId, status, body.length(),
                        response.uri()
                );

                if (status >= 200 && status < 300) {
                    if (body.isBlank()) {
                        throw new IllegalStateException(
                                "MNB returned an empty HTTP response. " +
                                "requestId=" + requestId
                        );
                    }

                    return parseResponse(body, requestId);
                }

                String diagnostic = truncate(body, 4000);

                log.error(
                        "[MNB:{}] HTTP {} from {}. Response body: {}",
                        requestId, status, endpoint,
                        diagnostic.isBlank()
                                ? "<empty response body>"
                                : diagnostic
                );

                failures.add(endpoint + " returned HTTP " + status
                        + "; body=" + diagnostic);

                // Try the alternative endpoint on 404.
                if (status == 404 && i + 1 < ENDPOINTS.size()) {
                    log.warn(
                            "[MNB:{}] HTTP 404; trying alternative endpoint.",
                            requestId
                    );
                    continue;
                }

                // Retry another endpoint for server errors.
                if (status >= 500 && i + 1 < ENDPOINTS.size()) {
                    continue;
                }

                throw new IllegalStateException(
                        "MNB request failed. HTTP status=" + status
                                + ", endpoint=" + endpoint
                                + ", requestId=" + requestId
                                + ", responseBody=" + diagnostic
                );

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "MNB request interrupted. requestId=" + requestId,
                        ex
                );

            } catch (java.io.IOException ex) {
                log.error(
                        "[MNB:{}] Network error calling {}: {}",
                        requestId, endpoint, ex.getMessage(), ex
                );

                failures.add(endpoint + " network error: "
                        + ex.getClass().getSimpleName()
                        + ": " + ex.getMessage());

                if (i + 1 < ENDPOINTS.size()) {
                    log.warn(
                            "[MNB:{}] Trying next endpoint after network error.",
                            requestId
                    );
                    continue;
                }

            } catch (IllegalStateException ex) {
                throw ex;

            } catch (Exception ex) {
                log.error(
                        "[MNB:{}] Unexpected integration failure: {}",
                        requestId, ex.getMessage(), ex
                );

                throw new IllegalStateException(
                        "MNB integration error. requestId=" + requestId
                                + ", reason=" + ex.getMessage(),
                        ex
                );
            }
        }

        throw new IllegalStateException(
                "All MNB endpoints failed. requestId=" + requestId
                        + ". Diagnostics: " + String.join(" | ", failures)
        );
    }

    private void validate(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required."
            );
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date must be on or before end date."
            );
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "EUR";
        }

        String normalized = currency.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");

        if (!normalized.matches("[A-Z]{3}(,[A-Z]{3})*")) {
            throw new IllegalArgumentException(
                    "Invalid currency format. Use EUR or EUR,USD,GBP."
            );
        }

        return normalized;
    }

    private String buildEnvelope(
            String currencies,
            LocalDate start,
            LocalDate end) {

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:web="%s">
                    <soap:Header/>
                    <soap:Body>
                        <web:GetExchangeRates>
                            <web:startDate>%s</web:startDate>
                            <web:endDate>%s</web:endDate>
                            <web:currencyNames>%s</web:currencyNames>
                        </web:GetExchangeRates>
                    </soap:Body>
                </soap:Envelope>
                """.formatted(SOAP_NAMESPACE, start, end, currencies);
    }

    private List<Map<String, String>> parseResponse(
            String soapXml,
            String requestId) throws Exception {

        Document soap = parseXml(soapXml);

        NodeList faults = soap.getElementsByTagNameNS("*", "Fault");

        if (faults.getLength() > 0) {
            String fault = faults.item(0).getTextContent().trim();

            log.error("[MNB:{}] SOAP fault: {}", requestId, fault);

            throw new IllegalStateException(
                    "MNB SOAP fault. requestId=" + requestId
                            + ", fault=" + fault
            );
        }

        NodeList results = soap.getElementsByTagNameNS(
                "*", "GetExchangeRatesResult"
        );

        if (results.getLength() == 0) {
            log.error(
                    "[MNB:{}] Missing GetExchangeRatesResult. XML={}",
                    requestId, truncate(soapXml, 4000)
            );

            throw new IllegalStateException(
                    "MNB response did not contain GetExchangeRatesResult. "
                            + "requestId=" + requestId
            );
        }

        String ratesXml = results.item(0).getTextContent().trim();

        if (ratesXml.isBlank()) {
            log.warn(
                    "[MNB:{}] Exchange-rate result is empty.",
                    requestId
            );
            return Collections.emptyList();
        }

        Document ratesDocument = parseXml(ratesXml);

        NodeList rateNodes = ratesDocument.getElementsByTagNameNS(
                "*", "Rate"
        );

        List<Map<String, String>> rates = new ArrayList<>();

        for (int i = 0; i < rateNodes.getLength(); i++) {
            Node node = rateNodes.item(i);

            if (!(node instanceof Element rate)) {
                continue;
            }

            String currency = rate.getAttribute("curr");
            String unit = rate.getAttribute("unit");
            String value = rate.getTextContent()
                    .trim().replace(",", ".");
            String date = findDayDate(rate);

            if (currency.isBlank() || unit.isBlank() || value.isBlank()) {
                log.warn(
                        "[MNB:{}] Skipping incomplete rate element.",
                        requestId
                );
                continue;
            }

            Map<String, String> row = new LinkedHashMap<>();
            row.put("date", date);
            row.put("currency", currency);
            row.put("unit", unit);
            row.put("value", value);

            rates.add(row);
        }

        log.info(
                "[MNB:{}] Successfully parsed {} rate rows.",
                requestId, rates.size()
        );

        return rates;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING, true
        );
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        return factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(xml))
        );
    }

    private String findDayDate(Element rate) {
        Node node = rate;

        while (node != null) {
            if (node instanceof Element element) {
                String name = element.getLocalName();

                if (name == null) {
                    name = element.getTagName();
                }

                if ("Day".equalsIgnoreCase(name)
                        && element.hasAttribute("date")) {
                    return element.getAttribute("date");
                }
            }

            node = node.getParentNode();
        }

        return "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String clean = text.replaceAll("[\\r\\n\\t]+", " ").trim();

        return clean.length() <= maxLength
                ? clean
                : clean.substring(0, maxLength) + "... [truncated]";
    }
}
