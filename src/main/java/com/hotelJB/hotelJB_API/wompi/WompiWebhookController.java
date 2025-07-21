package com.hotelJB.hotelJB_API.wompi;

import com.hotelJB.hotelJB_API.Dte.company.Company;
import com.hotelJB.hotelJB_API.Dte.company.CompanyRepository;
import com.hotelJB.hotelJB_API.models.dtos.ReservationDTO;
import com.hotelJB.hotelJB_API.models.entities.Reservation;
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

    @Autowired
    private CompanyRepository companyRepository;

    private static final String RESULT_SUCCESS = "ExitosaAprobada";

    @PostMapping
    public ResponseEntity<String> handleWompiWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("✅ Webhook recibido de Wompi: " + payload);

        try {
            Map<String, Object> enlacePago = (Map<String, Object>) payload.get("EnlacePago");
            String reservationCode = (enlacePago != null) ? (String) enlacePago.get("IdentificadorEnlaceComercio") : null;
            String resultado = (String) payload.get("ResultadoTransaccion");

            System.out.println("ReservationCode recibido: " + reservationCode);
            System.out.println("Resultado transacción: " + resultado);

            if (reservationCode != null && RESULT_SUCCESS.equals(resultado)) {
                ReservationResponse reservationResponse = reservationService.getByReservationCode(reservationCode);

                if (reservationResponse != null) {
                    Reservation reservationEntity = reservationService.findEntityById(reservationResponse.getReservationId());
                    ReservationDTO dto = reservationService.toDto(reservationEntity);

                    // Actualizar estado
                    reservationEntity.setStatus(dto.getInitDate().isAfter(java.time.LocalDate.now()) ? "FUTURA" : "ACTIVA");
                    reservationService.saveEntity(reservationEntity);

                    // Consultar empresa
                    Company company = companyRepository.findFirstBy().orElse(null);

                    // Si DTE está habilitado: generar y enviar DTE (que ya incluye correo con adjunto)
                    if (company != null && company.isDteEnabled()) {
                        reservationService.generateAndSendDte(reservationEntity.getReservationId());
                        System.out.println("DTE generado y correo enviado con adjunto.");
                    } else {
                        // Si DTE está desactivado: enviar solo correo
                        String htmlBody = reservationService.buildReservationEmailBody(dto);
                        emailSenderService.sendMail(dto.getEmail(), "Confirmación de Reserva - Jardines de las Marías", htmlBody);
                        System.out.println("Correo de reserva enviado sin DTE.");
                    }

                    // WhatsApp
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
                                dto.getName(), roomName, formattedInitDate, formattedFinishDate,
                                dto.getCantPeople(), dto.getReservationCode()
                        );

                        String rawPhone = dto.getPhone();
                        String cleanedPhone = rawPhone.replaceAll("[^\\d+]", "");
                        if (!cleanedPhone.startsWith("+")) {
                            cleanedPhone = "+503" + cleanedPhone;
                        }

                        whatsappService.sendWhatsAppMessage(cleanedPhone, message);
                        System.out.println("✅ WhatsApp enviado a " + dto.getPhone());
                    } catch (Exception e) {
                        System.out.println("❌ Error al enviar WhatsApp: " + e.getMessage());
                    }

                } else {
                    System.out.println("❌ No se encontró la reserva con código: " + reservationCode);
                }

            } else {
                System.out.println("❌ Pago fallido, anulado o sin código.");
            }

        } catch (Exception e) {
            System.out.println("🚨 Error procesando webhook de Wompi:");
            e.printStackTrace();
        }

        return ResponseEntity.ok("ok");
    }
}
