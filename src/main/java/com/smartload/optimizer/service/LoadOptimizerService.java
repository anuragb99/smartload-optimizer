package com.smartload.optimizer.service;

import com.smartload.optimizer.model.OptimizationResult;
import com.smartload.optimizer.model.dto.OptimizeRequest;
import com.smartload.optimizer.model.dto.OptimizeResponse;
import com.smartload.optimizer.validator.RequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Orchestrates validation → optimization → response mapping.
 * Stateless: no fields mutated after construction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadOptimizerService {

    private final RequestValidator validator;
    private final LoadOptimizer optimizer;

    public OptimizeResponse optimize(OptimizeRequest request) {
        log.info("Optimizing load for truck={}, orders={}", request.getTruck().getId(), request.getOrders().size());

        validator.validate(request);

        long start = System.currentTimeMillis();
        OptimizationResult result = optimizer.optimize(request.getTruck(), request.getOrders());
        log.info("Output completed in {} ms — selected {} orders, payout {} cents",
                System.currentTimeMillis() - start,
                result.getSelectedOrderIds().size(),
                result.getTotalPayoutCents());

        return toResponse(request.getTruck().getId(),
                    request.getTruck().getMaxWeightLbs(),
                    request.getTruck().getMaxVolumeCuft(),
                    result);
    }

    // Mapping Response
    private OptimizeResponse toResponse(String truckId, int maxWeight, int maxVolume,
                                        OptimizationResult result) {
        double utilizationWeight = round2(
                result.getTotalWeightLbs() * 100.0 / maxWeight);
        double utilizationVolume = round2(
                result.getTotalVolumeCuft() * 100.0 / maxVolume);

        List<String> ids = result.isEmpty() ? List.of() : result.getSelectedOrderIds();

        return OptimizeResponse.builder()
                .truckId(truckId)
                .selectedOrderIds(ids)
                .totalPayoutCents(result.getTotalPayoutCents())
                .totalWeightLbs(result.getTotalWeightLbs())
                .totalVolumeCuft(result.getTotalVolumeCuft())
                .utilizationWeightPercent(utilizationWeight)
                .utilizationVolumePercent(utilizationVolume)
                .build();
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
