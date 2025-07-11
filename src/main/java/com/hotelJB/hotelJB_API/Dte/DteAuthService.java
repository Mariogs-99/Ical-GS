package com.hotelJB.hotelJB_API.Dte;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DteAuthService {

    @Value("${dte.mh.nit}")
    private String nit;

    @Value("${dte.mh.password}")
    private String password;

    private static final String AUTH_URL = "https://apitest.dtes.mh.gob.sv/seguridad/auth";

    public String obtenerToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", nit);
        formData.add("pwd", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                AUTH_URL,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new RuntimeException("Respuesta vacía de autenticación.");
        }

        String status = (String) body.get("status");
        if (!"OK".equalsIgnoreCase(status)) {
            Object errorBody = body.get("body");
            throw new RuntimeException("Error autenticando en MH: " + errorBody);
        }

        Map<String, Object> innerBody = (Map<String, Object>) body.get("body");
        String token = (String) innerBody.get("token");

        if (token == null) {
            throw new RuntimeException("MH no devolvió token.");
        }

        // Limpiar "Bearer " si viene incluido
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        System.out.println("✅ Token limpio: " + token);

        return token;
    }


}
