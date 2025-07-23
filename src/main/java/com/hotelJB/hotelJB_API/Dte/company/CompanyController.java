package com.hotelJB.hotelJB_API.Dte.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

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

}
