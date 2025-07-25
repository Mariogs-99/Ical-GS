package com.hotelJB.hotelJB_API.Dte.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Value("${firmador.url}")
    private String firmadorUrl;

    @GetMapping
    public ResponseEntity<CompanyResponseDTO> getCompany() {
        Company company = companyService.getCompany();

        CompanyResponseDTO dto = new CompanyResponseDTO(
                company.getName(),
                company.getNombreComercial(),
                company.getCorreo(),
                company.getTelefono(),
                company.getDireccion(),
                company.getNit(),
                company.getNrc(),
                company.getDepartamento(),
                company.getMunicipio(),
                company.isDteEnabled(),
                company.getCodEstableMh(),
                company.getCodEstable(),
                company.getCodPuntoVentaMh(),
                company.getCodPuntoVenta()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<CompanyResponseDTO> updateCompany(@RequestBody UpdateCompanyRequest request) {
        Company updated = companyService.updateCompany(request);

        CompanyResponseDTO dto = new CompanyResponseDTO(
                updated.getName(),
                updated.getNombreComercial(),
                updated.getCorreo(),
                updated.getTelefono(),
                updated.getDireccion(),
                updated.getNit(),
                updated.getNrc(),
                updated.getDepartamento(),
                updated.getMunicipio(),
                updated.isDteEnabled(),
                updated.getCodEstableMh(),
                updated.getCodEstable(),
                updated.getCodPuntoVentaMh(),
                updated.getCodPuntoVenta()
        );

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/upload-cert")
    public ResponseEntity<String> uploadCertToSigner(@RequestParam("file") MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("certificado", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(firmadorUrl, requestEntity, String.class);

            return ResponseEntity.ok("Firmador respondió: " + response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al subir al firmador: " + e.getMessage());
        }
    }

}
