package com.hotelJB.hotelJB_API.Ical;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ical")
public class AirbnbIcalMockController {

    @GetMapping(value = "/airbnb.ics", produces = "text/calendar; charset=UTF-8")
    public ResponseEntity<byte[]> getIcal() {
        String ical = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Airbnb Test//EN
                CALSCALE:GREGORIAN
                BEGIN:VEVENT
                DTSTART:20250715T160000Z
                DTEND:20250719T160000Z
                SUMMARY:Mario Escobar
                UID:airbnb_ABC123XYZ
                DESCRIPTION:Room: Habitacion Individual\\nGuests: 2\\nTotal: $320.50\\nRoomNumber: 102-B
                END:VEVENT
                BEGIN:VEVENT
                DTSTART:20250722T160000Z
                DTEND:20250724T160000Z
                SUMMARY:Claudia Martinez
                UID:airbnb_DEF456UVW
                DESCRIPTION:Room: Habitacion Matrimonial\\nGuests: 3\\nTotal: $450.75\\nRoomNumber: 205-C
                END:VEVENT
                END:VCALENDAR
                """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=UTF-8")
                .body(ical.getBytes(StandardCharsets.UTF_8));
    }
}
