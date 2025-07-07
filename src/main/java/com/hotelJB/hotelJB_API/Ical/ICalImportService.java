package com.hotelJB.hotelJB_API.Ical;

import biweekly.Biweekly;
import biweekly.component.VEvent;
import com.hotelJB.hotelJB_API.models.entities.OtaIcalConfig;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
import com.hotelJB.hotelJB_API.models.entities.ReservationRoom;
import com.hotelJB.hotelJB_API.models.entities.Room;
import com.hotelJB.hotelJB_API.repositories.ReservationRepository;
import com.hotelJB.hotelJB_API.repositories.ReservationRoomRepository;
import com.hotelJB.hotelJB_API.repositories.RoomRepository;
import com.hotelJB.hotelJB_API.services.OtaIcalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ICalImportService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRoomRepository reservationRoomRepository;

    @Autowired
    private OtaIcalConfigService otaIcalConfigService;

    public void importFromUrl(String icalUrl, String otaName) throws Exception {
        System.out.println("🌐 Importando OTA [" + otaName + "] desde URL: " + icalUrl);

        InputStream inputStream = new URL(icalUrl).openStream();
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        var calendar = Biweekly.parse(reader).first();

        if (calendar == null) {
            System.out.println("⚠ No se pudo leer el calendario iCal de " + otaName + ". Está vacío o es inválido.");
            return;
        }

        for (VEvent event : calendar.getEvents()) {
            LocalDate initDate = null;
            LocalDate finishDate = null;

            if (event.getDateStart() != null && event.getDateStart().getValue() != null) {
                initDate = event.getDateStart().getValue()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            if (event.getDateEnd() != null && event.getDateEnd().getValue() != null) {
                finishDate = event.getDateEnd().getValue()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            String summary = event.getSummary() != null ? event.getSummary().getValue() : "";
            String uid = event.getUid() != null ? event.getUid().getValue() : "";
            String description = event.getDescription() != null ? event.getDescription().getValue() : "";

            // Ignorar bloqueos de disponibilidad de Airbnb (Not available)
            if (summary.toLowerCase().contains("not available")) {
                System.out.println("⏭ Evento ignorado (bloqueo de disponibilidad): " + summary);
                continue;
            }

            boolean exists = reservationRepository
                    .findByReservationCode(uid)
                    .isPresent();

            if (!exists && uid != null && !uid.isEmpty()) {
                String roomName = "Sin habitación";
                int guests = 0;
                double payment = 0.0;
                String roomNumber = null;

                if (!description.isEmpty()) {
                    description = description
                            .replace("\\n", "\n")
                            .replace("\\r", "\n")
                            .replace("\r\n", "\n")
                            .replace("\r", "\n");

                    for (String line : description.split("\n")) {
                        if (line.startsWith("Room:")) {
                            roomName = line.replace("Room:", "").trim();
                        } else if (line.startsWith("Guests:")) {
                            try {
                                guests = Integer.parseInt(line.replace("Guests:", "").trim());
                            } catch (NumberFormatException e) {
                                guests = 0;
                            }
                        } else if (line.startsWith("Total:")) {
                            String amountStr = line.replace("Total:", "")
                                    .replace("$", "")
                                    .trim();
                            try {
                                payment = Double.parseDouble(amountStr);
                            } catch (NumberFormatException e) {
                                payment = 0.0;
                            }
                        } else if (line.startsWith("RoomNumber:")) {
                            roomNumber = line.replace("RoomNumber:", "").trim();
                        }
                    }
                }

                Room room = null;
                if (!roomName.equals("Sin habitación")) {
                    System.out.println("🔎 Buscando habitación: [" + roomName + "]");

                    room = roomRepository.findByNameEsIgnoreCaseTrim(roomName).orElse(null);

                    if (room != null) {
                        System.out.println("✅ Room encontrado: id = " + room.getRoomId() +
                                ", nombre = " + room.getNameEs());
                    } else {
                        System.out.println("❌ Room NO encontrado para nombre: [" + roomName + "]");
                    }
                }

                Reservation newReservation = new Reservation();
                newReservation.setInitDate(initDate);
                newReservation.setFinishDate(finishDate);
                newReservation.setName(summary.isEmpty() ? "External OTA Reservation" : summary);
                newReservation.setReservationCode(uid);
                newReservation.setStatus("EXTERNAL");
                newReservation.setCantPeople(guests);
                newReservation.setPayment(payment);
                newReservation.setRoom(room);
                newReservation.setRoomNumber(roomNumber);
                newReservation.setQuantityReserved(1);

                reservationRepository.save(newReservation);

                if (room != null) {
                    ReservationRoom rr = new ReservationRoom();
                    rr.setReservation(newReservation);
                    rr.setRoom(room);
                    rr.setQuantity(1);
                    rr.setAssignedRoomNumber(roomNumber);
                    reservationRoomRepository.save(rr);

                    System.out.println("✅ Guardado también en reservation_room: reservationId = "
                            + newReservation.getReservationId() + ", roomId = " + room.getRoomId());
                }

                System.out.println("✅ Importada reserva UID: " + uid +
                        " (" + initDate + " → " + finishDate + ")" +
                        " Room: " + roomName +
                        " Guests: " + guests +
                        " Total: $" + payment +
                        " RoomNumber: " + roomNumber);
            }
        }
    }

    @Scheduled(fixedRate = 1800000) // Cada 30 minutos
    public void scheduledImport() {
        List<OtaIcalConfig> configs = otaIcalConfigService.getAllActiveConfigs();

        for (OtaIcalConfig config : configs) {
            if (config.getIcalUrl() == null || config.getIcalUrl().isBlank()) {
                System.out.println("⚠ OTA [" + config.getOtaName() + "] no tiene URL configurada. Se omite.");
                continue;
            }

            try {
                importFromUrl(config.getIcalUrl(), config.getOtaName());
            } catch (Exception e) {
                System.err.println("⚠ Error importando OTA " + config.getOtaName() +
                        " → " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
