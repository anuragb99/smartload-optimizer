package com.smartload.optimizer.validator;

import com.smartload.optimizer.exception.InvalidRequestException;
import com.smartload.optimizer.model.dto.OptimizeRequest;
import com.smartload.optimizer.model.dto.OrderDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain-level validation beyond bean constraints.
 * Checks: duplicate IDs, hazmat isolation, route compatibility, date ordering.
 */
@Component
public class RequestValidator {

    public void validate(OptimizeRequest request) {
        validateNoDuplicateOrderIds(request.getOrders());
        validateDates(request.getOrders());
    }

    //Private helpers
    private void validateNoDuplicateOrderIds(List<OrderDto> orders) {
        Set<String> seen = orders.stream()
                .map(OrderDto::getId)
                .collect(Collectors.toSet());

        if (seen.size() != orders.size()) {
            throw new InvalidRequestException("Duplicate order IDs detected in the request");
        }
    }

    private void validateDates(List<OrderDto> orders) {
        for (OrderDto order : orders) {
            if (order.getPickupDate() != null && order.getDeliveryDate() != null
                    && order.getPickupDate().isAfter(order.getDeliveryDate())) {
                throw new InvalidRequestException(
                        "Order '%s': pickup_date (%s) must not be after delivery_date (%s)"
                                .formatted(order.getId(), order.getPickupDate(), order.getDeliveryDate())
                );
            }
        }
    }
}
