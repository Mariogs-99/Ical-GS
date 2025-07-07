package com.hotelJB.hotelJB_API.models.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OtaIcalConfigResponse {
    private Long id;
    private String otaName;
    private Boolean active;
    private LocalDateTime updatedAt;
}
