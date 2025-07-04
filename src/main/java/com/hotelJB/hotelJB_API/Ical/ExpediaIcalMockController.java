package com.hotelJB.hotelJB_API.Ical;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ical")
public class ExpediaIcalMockController {

    @GetMapping(value = "/expedia.ics", produces = "text/calendar; charset=UTF-8")
    public ResponseEntity<byte[]> getIcal() {
        String ical = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Expedia Test//EN
                CALSCALE:GREGORIAN
                BEGIN:VEVENT
                DTSTART:20250710T160000Z
                DTEND:20250713T160000Z
                SUMMARY:Laura Gomez
                UID:expedia_XYZ789
                DESCRIPTION:Room: Habitacion Triple\\nGuests: 3\\nTotal: $510.25\\nRoomNumber: 303-D
                END:VEVENT
                BEGIN:VEVENT
                DTSTART:20250725T160000Z
                DTEND:20250727T160000Z
                SUMMARY:Carlos Ruiz
                UID:expedia_MNO456
                DESCRIPTION:Room: Habitacion Familiar\\nGuests: 5\\nTotal: $700.00\\nRoomNumber: 401-E
                END:VEVENT
                END:VCALENDAR
                """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=UTF-8")
                .body(ical.getBytes(StandardCharsets.UTF_8));
    }
}
