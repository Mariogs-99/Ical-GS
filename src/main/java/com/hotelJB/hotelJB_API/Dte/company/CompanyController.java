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
    public ResponseEntity<Company> getCompany() {
        return ResponseEntity.ok(companyService.getCompany());
    }

    @PutMapping
    public ResponseEntity<Company> updateCompany(@RequestBody UpdateCompanyRequest request) {
        Company updated = companyService.updateCompany(request);
        return ResponseEntity.ok(updated);
    }
}
