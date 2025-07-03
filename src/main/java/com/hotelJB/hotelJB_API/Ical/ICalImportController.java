package com.hotelJB.hotelJB_API.Ical;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ical")
public class ICalImportController {

    @Autowired
    private ICalImportService importService;

    @PostMapping("/import")
    public String importIcal(@RequestParam String url) throws Exception {
        importService.importFromUrl(url);
        return "Importación completada.";
    }
}

