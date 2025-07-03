package com.hotelJB.hotelJB_API.Ical;

import biweekly.Biweekly;
import biweekly.component.VEvent;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
import com.hotelJB.hotelJB_API.repositories.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ICalImportService {

    @Autowired
    private ReservationRepository reservationRepository;

    public void importFromUrl(String icalUrl) throws Exception {
        InputStream inputStream = new URL(icalUrl).openStream();

        var calendar = Biweekly.parse(inputStream).first();

        for (VEvent event : calendar.getEvents()) {
            LocalDate initDate = event.getDateStart().getValue()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            LocalDate finishDate = event.getDateEnd().getValue()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            String summary = event.getSummary() != null ? event.getSummary().getValue() : "";
            String uid = event.getUid() != null ? event.getUid().getValue() : "";

            // Verificar si ya existe
            boolean exists = reservationRepository
                    .findByReservationCode(uid)
                    .isPresent();

            if (!exists && uid != null && !uid.isEmpty()) {
                Reservation newReservation = new Reservation();
                newReservation.setInitDate(initDate);
                newReservation.setFinishDate(finishDate);
                newReservation.setName(summary.isEmpty() ? "External OTA Reservation" : summary);
                newReservation.setReservationCode(uid);
                newReservation.setStatus("EXTERNAL");

                reservationRepository.save(newReservation);

                System.out.println("Importada reserva UID: " + uid +
                        " (" + initDate + " → " + finishDate + ")");
            }
        }
    }

    @Scheduled(fixedRate = 1800000) // Cada 30 minutos
    public void scheduledImport() {
        try {
            importFromUrl("https://ical.booking.com/ical/abc123.ics");
            importFromUrl("https://airbnb.com/ical/xyz987.ics");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
