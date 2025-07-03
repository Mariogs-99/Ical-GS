package com.hotelJB.hotelJB_API.Ical;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
import com.hotelJB.hotelJB_API.services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class ICalExportService {

    @Autowired
    private ReservationService reservationService;

    public String generateICal() {
        ICalendar ical = new ICalendar();

        List<Reservation> reservations = reservationService.getAll();

        for (Reservation res : reservations) {
            VEvent event = new VEvent();

            event.setDateStart(Date.from(
                    res.getInitDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            ));

            event.setDateEnd(Date.from(
                    res.getFinishDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            ));

            event.setSummary(res.getName());
            event.setUid(res.getReservationCode());

            String description = String.format(
                    "Room: %s\nPhone: %s\nEmail: %s\nPayment: %.2f\nStatus: %s",
                    res.getRoom() != null ? res.getRoom().getNameEs() : "N/A",
                    res.getPhone(),
                    res.getEmail(),
                    res.getPayment(),
                    res.getStatus()
            );

            event.setDescription(description);

            ical.addEvent(event);
        }

        return Biweekly.write(ical).go();
    }
}
