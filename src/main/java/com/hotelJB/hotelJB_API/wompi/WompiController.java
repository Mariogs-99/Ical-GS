package com.hotelJB.hotelJB_API.wompi;

import com.hotelJB.hotelJB_API.models.dtos.ReservationDTO;
import com.hotelJB.hotelJB_API.models.responses.ReservationResponse;
import com.hotelJB.hotelJB_API.services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/reservations")
public class WompiController {

    @Autowired
    private WompiService wompiService;

    @Autowired
    private ReservationService reservationService;

    /**
     * Endpoint para crear un enlace de pago en Wompi.
     * Ahora sin usar Redis ni reserva temporal.
     */
    @PostMapping("/wompi/link")
    public ResponseEntity<?> createWompiLink(@RequestBody ReservationDTO reservationDTO) throws Exception {
        // Crear la reserva en estado PENDIENTE
        ReservationResponse savedReservation =
                reservationService.saveWithStatus(reservationDTO, "PENDIENTE");

        String reservationCode = savedReservation.getReservationCode();
        reservationDTO.setReservationCode(reservationCode);

        // Usar el reservationCode como IdentificadorEnlaceComercio
        String wompiUrl = wompiService.crearEnlacePago(reservationDTO, reservationCode, reservationCode);

        Map<String, Object> response = new HashMap<>();
        response.put("reservationCode", reservationCode);
        response.put("urlPagoWompi", wompiUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener reserva por reservationCode.
     */
    @GetMapping("/by-code/{reservationCode}")
    public ResponseEntity<ReservationResponse> getByCode(@PathVariable String reservationCode) throws Exception {
        ReservationResponse reservation = reservationService.getByReservationCode(reservationCode);
        return ResponseEntity.ok(reservation);
    }
}

