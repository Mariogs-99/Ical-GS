package com.hotelJB.hotelJB_API.Ical;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ical")
public class ICalExportController {

    @Autowired
    private ICalExportService exportService;

    @GetMapping("/export")
    public ResponseEntity<String> exportIcal() {
        String icalContent = exportService.generateICal();

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .body(icalContent);
    }
}
