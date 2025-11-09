package com.hungerexpress.customer;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String profilePictureUrl;
    private Boolean profileCompleted;
    private Instant createdAt;
}
