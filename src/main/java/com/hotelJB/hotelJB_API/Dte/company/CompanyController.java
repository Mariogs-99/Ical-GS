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
    public ResponseEntity<Company> updateDteStatus(@RequestBody DteToggleRequest request) {
        Company updated = companyService.updateDteEnabled(request.isDteEnabled());
        return ResponseEntity.ok(updated);
    }

    public static class DteToggleRequest {
        private boolean dteEnabled;

        public boolean isDteEnabled() {
            return dteEnabled;
        }

        public void setDteEnabled(boolean dteEnabled) {
            this.dteEnabled = dteEnabled;
        }
    }
}
