package com.hotelJB.hotelJB_API.Dte;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DteSignerService {

    public String firmar(String dteJson) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("documento", dteJson);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:8113/firmardocumento", //firmador local
                HttpMethod.POST,
                request,
                Map.class
        );

        String dteFirmado = (String) response.getBody().get("documento");
        return dteFirmado;
    }
}

