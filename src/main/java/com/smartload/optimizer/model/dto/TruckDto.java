package com.smartload.optimizer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TruckDto {

    @NotBlank(message = "truck id must not be blank")
    private String id;

    @Positive(message = "truck weight must be positive")
    @JsonProperty("max_weight_lbs")
    private Integer maxWeightLbs;

    @Positive(message = "truck volume must be positive")
    @JsonProperty("max_volume_cuft")
    private Integer maxVolumeCuft;
}
