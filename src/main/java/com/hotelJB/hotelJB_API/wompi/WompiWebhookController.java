package com.hotelJB.hotelJB_API.wompi;

import com.hotelJB.hotelJB_API.models.dtos.ReservationDTO;
import com.hotelJB.hotelJB_API.models.responses.ReservationResponse;
import com.hotelJB.hotelJB_API.services.EmailSenderService;
import com.hotelJB.hotelJB_API.services.ReservationService;
import com.hotelJB.hotelJB_API.services.RoomService;
import com.hotelJB.hotelJB_API.twilio.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/webhook-wompi")
public class WompiWebhookController {

    @Autowired
    private TempReservationService tempReservationService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private WhatsAppService whatsappService;

    @Autowired
    private RoomService roomService;

    private static final String RESULT_SUCCESS = "ExitosaAprobada";

    @PostMapping
    public ResponseEntity<String> handleWompiWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("✅ Webhook recibido de Wompi: " + payload);

        try {
            Map<String, Object> enlacePago = (Map<String, Object>) payload.get("EnlacePago");
            String tempReference = (enlacePago != null) ? (String) enlacePago.get("IdentificadorEnlaceComercio") : null;
            String resultado = (String) payload.get("ResultadoTransaccion");

            System.out.println("Referencia recibida: " + tempReference);
            System.out.println("Resultado transacción: " + resultado);

            if (tempReference != null && resultado != null && tempReference.startsWith("Temp-")) {
                if (RESULT_SUCCESS.equals(resultado)) {
                    ReservationDTO dto = tempReservationService.getTempReservation(tempReference);

                    if (dto != null && dto.getReservationCode() != null && !dto.getReservationCode().isBlank()) {
                        ReservationResponse reservationResponse =
                                reservationService.getByReservationCode(dto.getReservationCode());

                        if (reservationResponse != null) {
                            if (dto.getInitDate().isAfter(java.time.LocalDate.now())) {
                                dto.setStatus("FUTURA");
                            } else {
                                dto.setStatus("ACTIVA");
                            }

                            reservationService.update(dto, reservationResponse.getReservationId());
                            reservationService.generateAndSendDte(reservationResponse.getReservationId());

                            try {
                                String formattedInitDate = dto.getInitDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                String formattedFinishDate = dto.getFinishDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                                Long roomId = dto.getRooms().get(0).getRoomId().longValue();
                                String roomName = roomService.getRoomNameById(roomId);

                                String message = String.format(
                                        "📢 *Confirmación de Reserva - Jardines de las Marías*\n\n" +
                                                "Hola %s 👋,\n\n" +
                                                "¡Tu reserva ha sido confirmada exitosamente! 🎉\n\n" +
                                                "🛏️ Habitación: %s\n" +
                                                "🗓️ Fechas: %s al %s\n" +
                                                "👥 Huéspedes: %d persona(s)\n" +
                                                "🔖 Código de reserva: %s\n\n" +
                                                "📍 Dirección: Km 7.5 Carretera Panorámica, San Salvador\n" +
                                                "📞 Teléfono: +503 7012 3456\n\n" +
                                                "¡Gracias por elegirnos! 🌿\n" +
                                                "Nos vemos pronto 🌄",
                                        dto.getName(),
                                        roomName,
                                        formattedInitDate,
                                        formattedFinishDate,
                                        dto.getCantPeople(),
                                        dto.getReservationCode()
                                );

                                //Convertidor
                                String rawPhone = dto.getPhone();
                                String cleanedPhone = rawPhone.replaceAll("[^\\d+]", "");

                                if (!cleanedPhone.startsWith("+")) {
                                    cleanedPhone = "+503" + cleanedPhone; // o el país que uses
                                }

                                whatsappService.sendWhatsAppMessage(cleanedPhone, message);

                                System.out.println("✅ WhatsApp enviado a " + dto.getPhone());

                            } catch (Exception e) {
                                System.out.println("❌ Error al enviar mensaje de WhatsApp: " + e.getMessage());
                            }

                            tempReservationService.deleteTempReservation(tempReference);

                        } else {
                            System.out.println("❌ No se encontró reserva real con código: " + dto.getReservationCode());
                        }
                    } else {
                        System.out.println("❌ DTO temporal inválido o sin código de reserva.");
                    }
                } else {
                    System.out.println("❌ Pago fallido o anulado.");
                }
            } else {
                System.out.println("❌ Referencia inválida o desconocida.");
            }

        } catch (Exception e) {
            System.out.println("🚨 Error procesando el webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok("ok");
        }

        return ResponseEntity.ok("ok");
    }
}
