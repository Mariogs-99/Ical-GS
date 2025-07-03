package com.hotelJB.hotelJB_API.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDTO {
    private String telephone;
    private String telephone2;
    private String email;
    private String address;
    private String addressUrl;
    private String facebookUsername;
    private String facebookUrl;
    private String instagramUsername;
    private String tiktok;
}

