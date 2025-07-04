package com.hotelJB.hotelJB_API.Ical;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ical")
public class BookingIcalMockController {

    @GetMapping(value = "/booking.ics", produces = "text/calendar; charset=UTF-8")
    public ResponseEntity<byte[]> getIcal() {
        String ical = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Booking Test//EN
                CALSCALE:GREGORIAN
                BEGIN:VEVENT
                DTSTART:20250709T160000Z
                DTEND:20250712T160000Z
                SUMMARY:John Doe
                UID:booking_987654321
                DESCRIPTION:Room: Habitacion Matrimonial\\nGuests: 2\\nTotal: $450.00
                END:VEVENT
                BEGIN:VEVENT
                DTSTART:20250718T160000Z
                DTEND:20250720T160000Z
                SUMMARY:Jane Smith
                UID:booking_123456789
                DESCRIPTION:Room: Habitacion Familiar\\nGuests: 4\\nTotal: $580.00
                END:VEVENT
                END:VCALENDAR
                """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=UTF-8")
                .body(ical.getBytes(StandardCharsets.UTF_8));
    }
}
