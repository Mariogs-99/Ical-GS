package com.hotelJB.hotelJB_API.Dte;

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

    public String firmar(String dteJson) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("dteJson", dteJson);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                signerUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();

        // Valida respuesta
        if (body != null && "OK".equalsIgnoreCase((String) body.get("status"))) {
            return (String) body.get("body");
        } else {
            throw new RuntimeException("Error firmando DTE: " + response.getBody());
        }
    }
}
