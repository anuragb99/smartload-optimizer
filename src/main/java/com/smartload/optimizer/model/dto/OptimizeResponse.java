package com.smartload.optimizer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OptimizeResponse {

    @JsonProperty("truck_id")
    private String truckId;

    @JsonProperty("selected_order_ids")
    private List<String> selectedOrderIds;

    @JsonProperty("total_payout_cents")
    private Long totalPayoutCents;

    @JsonProperty("total_weight_lbs")
    private Integer totalWeightLbs;

    @JsonProperty("total_volume_cuft")
    private Integer totalVolumeCuft;

    @JsonProperty("utilization_weight_percent")
    private Double utilizationWeightPercent;

    @JsonProperty("utilization_volume_percent")
    private Double utilizationVolumePercent;
}
