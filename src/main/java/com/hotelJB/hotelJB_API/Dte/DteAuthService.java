package com.hotelJB.hotelJB_API.Dte;

import com.hotelJB.hotelJB_API.Dte.company.Company;
import com.hotelJB.hotelJB_API.Dte.company.CompanyService;
import com.hotelJB.hotelJB_API.Dte.company.EncryptionUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DteAuthService {

    private final CompanyService companyService;
    private final EncryptionUtil encryptionUtil;

    private static final String AUTH_URL = "https://apitest.dtes.mh.gob.sv/seguridad/auth";

    public DteAuthService(CompanyService companyService, EncryptionUtil encryptionUtil) {
        this.companyService = companyService;
        this.encryptionUtil = encryptionUtil;
    }

    public String obtenerToken() {
        RestTemplate restTemplate = new RestTemplate();

        Company company = companyService.getCompany();

        String nit = company.getNit();
        String encryptedPassword = company.getMhPassword();

        if (nit == null || encryptedPassword == null) {
            throw new RuntimeException("El NIT o la contraseña del MH no están configurados.");
        }

        //  Descifrar la contraseña
        String password;
        try {
            password = encryptionUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña del MH.", e);
        }

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

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        System.out.println("✅ Token limpio: " + token);
        return token;
    }
}
