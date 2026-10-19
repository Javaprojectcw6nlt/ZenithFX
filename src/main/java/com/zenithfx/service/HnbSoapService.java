package com.zenithfx.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class HnbSoapService {
    private static final String ENDPOINT = "https://www.mnb.hu/arfolyamok.asmx";
    private static final String SOAP_ACTION = "http://www.mnb.hu/webservices/GetExchangeRates";
    private final HttpClient client = HttpClient.newHttpClient();

    public List<Map<String, String>> getRates(String currency, LocalDate startDate, LocalDate endDate) throws Exception {
        if (startDate.isAfter(endDate)) throw new IllegalArgumentException("Start date must be on or before end date.");
        String safeCurrency = currency == null ? "EUR" : currency.replaceAll("[^A-Z]", "");
        if (safeCurrency.isBlank()) safeCurrency = "EUR";
        String envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <GetExchangeRates xmlns="http://www.mnb.hu/webservices/">
                  <startDate>%s</startDate>
                  <endDate>%s</endDate>
                  <currencyNames>%s</currencyNames>
                </GetExchangeRates>
              </soap:Body>
            </soap:Envelope>
            """.formatted(startDate, endDate, safeCurrency);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "\"" + SOAP_ACTION + "\"")
            .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("MNB SOAP service returned HTTP " + response.statusCode());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.body())));
        NodeList faults = document.getElementsByTagNameNS("*", "Fault");
        if (faults.getLength() > 0) throw new IllegalStateException("MNB SOAP fault: " + faults.item(0).getTextContent());

        NodeList rateNodes = document.getElementsByTagNameNS("*", "Rate");
        List<Map<String, String>> rates = new ArrayList<>();
        for (int i = 0; i < rateNodes.getLength(); i++) {
            Element rate = (Element) rateNodes.item(i);
            Map<String, String> row = new LinkedHashMap<>();
            row.put("date", nearestDayDate(rate));
            row.put("currency", rate.getAttribute("curr"));
            row.put("unit", rate.getAttribute("unit"));
            row.put("value", rate.getTextContent().trim().replace(",", "."));
            rates.add(row);
        }
        return rates;
    }

    private String nearestDayDate(Element rate) {
        Node node = rate;
        while (node != null && node instanceof Element) {
            Element element = (Element) node;
            if ("Day".equalsIgnoreCase(element.getLocalName()) && element.hasAttribute("date")) {
                return element.getAttribute("date");
            }
            node = node.getParentNode();
        }
        return "";
    }
}
