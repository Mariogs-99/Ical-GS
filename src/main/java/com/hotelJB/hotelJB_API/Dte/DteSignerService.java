package com.hotelJB.hotelJB_API.Dte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DteSignerService {

    @Value("${dte.signer.url}")
    private String signerUrl;

    @Value("${dte.signer.nit}")
    private String nitEmisor;

    @Value("${dte.signer.certPassword}")
    private String passwordCertificado;

    public String firmar(String dteJson) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dteJsonObject;
        try {
            dteJsonObject = mapper.readValue(dteJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo DTE a objeto JSON: " + e.getMessage(), e);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nit", nitEmisor);
        payload.put("passwordPri", passwordCertificado);
        payload.put("dteJson", dteJsonObject);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                signerUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();

        if (body != null && "OK".equalsIgnoreCase((String) body.get("status"))) {
            return (String) body.get("body");
        } else {
            throw new RuntimeException("Error firmando DTE: " + response.getBody());
        }
    }
}
