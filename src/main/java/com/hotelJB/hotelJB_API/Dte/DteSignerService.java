package com.hotelJB.hotelJB_API.Dte;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelJB.hotelJB_API.Dte.company.Company;
import com.hotelJB.hotelJB_API.Dte.company.CompanyService;
import com.hotelJB.hotelJB_API.Dte.company.EncryptionUtil;
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

    private final CompanyService companyService;
    private final EncryptionUtil encryptionUtil;

    public DteSignerService(CompanyService companyService, EncryptionUtil encryptionUtil) {
        this.companyService = companyService;
        this.encryptionUtil = encryptionUtil;
    }

    public String firmar(String dteJson) {
        RestTemplate restTemplate = new RestTemplate();

        Company company = companyService.getCompany();

        String nit = company.getNit();
        String encryptedCertPassword = company.getCertPassword();

        if (nit == null || encryptedCertPassword == null) {
            throw new RuntimeException("Faltan el NIT o la contraseña del certificado en la configuración del hotel.");
        }

        // Descifrar contraseña del certificado
        String certPassword;
        try {
            certPassword = encryptionUtil.decrypt(encryptedCertPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña del certificado.", e);
        }

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
        payload.put("nit", nit);
        payload.put("passwordPri", certPassword);
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
            throw new RuntimeException("Error firmando DTE: " + body);
        }
    }
}
