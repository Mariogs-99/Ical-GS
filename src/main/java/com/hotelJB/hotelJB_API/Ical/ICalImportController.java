package com.hotelJB.hotelJB_API.Ical;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ical")
public class ICalImportController {

    @Autowired
    private ICalImportService importService;

    /**
     * Permite disparar la importación manualmente desde una URL y nombre OTA.
     * Ejemplo de llamada:
     * POST /api/ical/import?url=https://miurl.ics&otaName=Airbnb
     */
    @PostMapping("/import")
    public String importIcal(@RequestParam String url,
                             @RequestParam String otaName) throws Exception {
        importService.importFromUrl(url, otaName);
        return "✅ Importación completada para OTA: " + otaName;
    }
}
