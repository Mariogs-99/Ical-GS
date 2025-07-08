package com.hotelJB.hotelJB_API.Dte;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DteTransmitterService {

    private static final String DTE_URL = "https://apitest.dtes.mh.gob.sv/recepciondte";

    public String enviarDte(String dteFirmadoJson, String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        HttpEntity<String> request = new HttpEntity<>(dteFirmadoJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                DTE_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}

