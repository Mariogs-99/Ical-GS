package com.hotelJB.hotelJB_API.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRoomDTO {
    private Integer roomId;
    private Integer quantity;
    private String assignedRoomNumber; // opcional
}
