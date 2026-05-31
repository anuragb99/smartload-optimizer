package com.smartload.optimizer.service;

import com.smartload.optimizer.model.OptimizationResult;
import com.smartload.optimizer.model.dto.OrderDto;
import com.smartload.optimizer.model.dto.TruckDto;

import java.util.List;

public interface LoadOptimizer {

    /**
     * Returns the optimal combination of compatible orders that maximises profit
     */
    OptimizationResult optimize(TruckDto truck, List<OrderDto> orders);
}
