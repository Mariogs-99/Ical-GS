package com.hotelJB.hotelJB_API.Dte;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelJB.hotelJB_API.Dte.dto.DteResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DteTransmitterService {

    private static final String DTE_URL = "https://apitest.dtes.mh.gob.sv/recepciondte";

    public DteResponse enviarDte(String dteFirmadoJson, String token) throws Exception {
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

        String responseBody = response.getBody();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(responseBody);

        String estado = jsonNode.path("estado").asText();
        String mensaje = jsonNode.path("mensaje").asText();
        String selloRecepcion = jsonNode.path("selloRecepcion").asText(null);
        String codigoGeneracion = jsonNode.path("codigoGeneracion").asText(null);

        // Procesar observaciones si hay errores
        StringBuilder obsBuilder = new StringBuilder();
        if (jsonNode.has("observaciones") && jsonNode.get("observaciones").isArray()) {
            for (JsonNode obs : jsonNode.get("observaciones")) {
                obsBuilder
                        .append(obs.path("codigoMsg").asText())
                        .append(": ")
                        .append(obs.path("descripcionMsg").asText())
                        .append("\n");
            }
        }

        boolean exitoso = "RECIBIDO".equalsIgnoreCase(estado);

        return new DteResponse(
                exitoso,
                estado,
                mensaje,
                codigoGeneracion,
                selloRecepcion,
                obsBuilder.toString(),
                responseBody
        );

    }
}
