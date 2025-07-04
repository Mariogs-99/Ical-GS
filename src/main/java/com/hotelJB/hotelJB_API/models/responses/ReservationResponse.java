package com.hotelJB.hotelJB_API.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private int reservationId;
    private String reservationCode;
    private LocalDate initDate;
    private LocalDate finishDate;
    private int cantPeople;
    private String name;
    private String email;
    private String phone;
    private double payment;
    private int quantityReserved;
    private LocalDateTime creationDate;
    private String status;

    // ✅ Para multi-room (sigue igual)
    private List<ReservationRoomResponse> rooms;

    // ✅ Nuevo campo para reservas EXTERNAS (una sola habitación)
    private RoomShortResponse room;

    private String roomNumber;
    private String dteControlNumber;
    private String wompiCheckoutUrl;
}
