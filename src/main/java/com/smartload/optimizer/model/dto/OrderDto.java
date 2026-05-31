package com.smartload.optimizer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderDto {

    @NotBlank(message = "order id must not be blank")
    private String id;

    @Positive(message = "payout_cents must be positive")
    @JsonProperty("payout_cents")
    private Long payoutCents;

    @Positive(message = "weight_lbs must be positive")
    @JsonProperty("weight_lbs")
    private Integer weightLbs;

    @Positive(message = "volume_cuft must be positive")
    @JsonProperty("volume_cuft")
    private Integer volumeCuft;

    @NotBlank(message = "origin must not be blank")
    private String origin;

    @NotBlank(message = "destination must not be blank")
    private String destination;

    @NotNull(message = "pickup_date must not be null")
    @JsonProperty("pickup_date")
    private LocalDate pickupDate;

    @NotNull(message = "delivery_date must not be null")
    @JsonProperty("delivery_date")
    private LocalDate deliveryDate;

    @JsonProperty("is_hazmat")
    private boolean isHazmat;
}
