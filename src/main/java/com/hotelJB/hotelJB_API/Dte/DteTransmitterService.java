package com.hotelJB.hotelJB_API.Dte;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelJB.hotelJB_API.Dte.dto.DteRequestDTO;
import com.hotelJB.hotelJB_API.Dte.dto.DteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class DteTransmitterService {

    private static final String DTE_URL = "https://apitest.dtes.mh.gob.sv/fesv/recepciondte";

    @Autowired
    private DteSignerService signerService;

    public DteResponse enviarDte(DteRequestDTO dteRequestDTO, String token) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        ObjectMapper mapper = new ObjectMapper();
        String jsonToSign = mapper.writeValueAsString(dteRequestDTO);

        System.out.println("🚀 JSON que se firmará:");
        System.out.println(jsonToSign);

        // Paso 1 → Firmar el DTE
        String jwt = signerService.firmar(jsonToSign);

        System.out.println("🔒 JWT firmado:");
        System.out.println(jwt);

        // Paso 2 → Preparar JSON que requiere Hacienda
        Map<String, Object> payload = new HashMap<>();
        payload.put("ambiente", dteRequestDTO.getIdentificacion().getAmbiente());
        payload.put("idEnvio", 1);
        payload.put("version", 1);
        payload.put("tipoDte", dteRequestDTO.getIdentificacion().getTipoDte());
        payload.put("documento", jwt);

        String jsonToSend = mapper.writeValueAsString(payload);

        System.out.println("📤 JSON que se enviará a Hacienda:");
        System.out.println(jsonToSend);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(jsonToSend, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    DTE_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();

            System.out.println("Respuesta Hacienda:");
            System.out.println(responseBody);

            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

            String estado = (String) responseMap.getOrDefault("estado", null);
            String descripcionMsg = (String) responseMap.getOrDefault("descripcionMsg", null); // nuevo
            String codigoMsg = (String) responseMap.getOrDefault("codigoMsg", null);           // nuevo
            String selloRecibido = (String) responseMap.getOrDefault("selloRecibido", null);
            String codigoGeneracion = (String) responseMap.getOrDefault("codigoGeneracion", null);


            boolean exitoso = "PROCESADO".equalsIgnoreCase(estado) && "001".equals(codigoMsg);


            return new DteResponse(
                    estado,
                    descripcionMsg,
                    codigoGeneracion,
                    selloRecibido,
                    Collections.singletonList(""),
                    responseBody
            );


        } catch (HttpClientErrorException e) {
            System.out.println("❌ ERROR al enviar DTE:");
            System.out.println(e.getResponseBodyAsString());
            throw e;
        }
    }

}
