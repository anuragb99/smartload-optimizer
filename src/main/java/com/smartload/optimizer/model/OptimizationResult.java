package com.smartload.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OptimizationResult {

    List<String> selectedOrderIds;
    long totalPayoutCents;
    int totalWeightLbs;
    int totalVolumeCuft;

    public boolean isEmpty() {
        return selectedOrderIds == null || selectedOrderIds.isEmpty();
    }
}
