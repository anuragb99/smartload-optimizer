package com.smartload.optimizer.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OptimizeRequest {

    @NotNull(message = "truck must not be null")
    @Valid
    private TruckDto truck;

    @NotEmpty(message = "orders must not be empty")
    @Size(max = 22, message = "orders list exceeds maximum of 22 (bitmask DP limit)")
    @Valid
    private List<OrderDto> orders;
}
