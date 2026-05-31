package com.smartload.optimizer.optimizer;

import com.smartload.optimizer.model.OptimizationResult;
import com.smartload.optimizer.model.dto.OrderDto;
import com.smartload.optimizer.model.dto.TruckDto;
import com.smartload.optimizer.service.BitmaskDPOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BitmaskDPOptimizerTest {

    private BitmaskDPOptimizer optimizer;
    private TruckDto truck;

    @BeforeEach
    void setUp() {
        optimizer = new BitmaskDPOptimizer();
        truck = new TruckDto();
        truck.setId("truck-123");
        truck.setMaxWeightLbs(44000);
        truck.setMaxVolumeCuft(3000);
    }

    @Test
    void selectsOptimalNonHazmatOrders() {
        List<OrderDto> orders = List.of(
                order("ord-001", 250000, 18000, 1200, false),
                order("ord-002", 180000, 12000, 900,  false),
                order("ord-003", 320000, 30000, 1800, true)   // hazmat — excluded from non-hazmat mix
        );

        OptimizationResult result = optimizer.optimize(truck, orders);

        assertThat(result.getSelectedOrderIds()).containsExactlyInAnyOrder("ord-001", "ord-002");
        assertThat(result.getTotalPayoutCents()).isEqualTo(430000L);
        assertThat(result.getTotalWeightLbs()).isEqualTo(30000);
        assertThat(result.getTotalVolumeCuft()).isEqualTo(2100);
    }

    @Test
    void respectsWeightCapacity() {
        List<OrderDto> orders = List.of(
                order("ord-heavy", 999999, 44001, 100, false),  // 1 lb over limit
                order("ord-light", 100000, 10000, 100, false)
        );

        OptimizationResult result = optimizer.optimize(truck, orders);

        assertThat(result.getSelectedOrderIds()).containsExactly("ord-light");
    }

    @Test
    void respectsVolumeCapacity() {
        List<OrderDto> orders = List.of(
                order("ord-bulk", 999999, 100, 3001, false),  // 1 cuft over limit
                order("ord-small", 100000, 100, 100, false)
        );

        OptimizationResult result = optimizer.optimize(truck, orders);

        assertThat(result.getSelectedOrderIds()).containsExactly("ord-small");
    }

    @Test
    void hazmatIsolation_hazmatOnlyIsValid() {
        List<OrderDto> orders = List.of(
                order("haz-1", 200000, 10000, 500, true),
                order("haz-2", 150000, 8000,  400, true)
        );

        OptimizationResult result = optimizer.optimize(truck, orders);

        assertThat(result.getSelectedOrderIds()).containsExactlyInAnyOrder("haz-1", "haz-2");
    }

    @Test
    void hazmatIsolation_mixedIsInvalid() {
        List<OrderDto> orders = List.of(
                order("haz-1",  300000, 10000, 500, true),
                order("norm-1", 100000, 8000,  400, false)
        );

        OptimizationResult result = optimizer.optimize(truck, orders);

        // Best feasible single-order pick: haz-1 is higher payout
        assertThat(result.getSelectedOrderIds()).containsExactly("haz-1");
        assertThat(result.getTotalPayoutCents()).isEqualTo(300000L);
    }

    @Test
    void emptyOrdersReturnsEmptyResult() {
        OptimizationResult result = optimizer.optimize(truck, List.of());

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalPayoutCents()).isZero();
    }

    @Test
    void noFeasibleCombinationReturnsEmptyResult() {
        List<OrderDto> orders = List.of(
                order("ord-x", 999999, 50000, 5000, false)  // both limits exceeded
        );

        OptimizationResult result = optimizer.optimize(truck, orders);
        assertThat(result.isEmpty()).isTrue();
    }

    private OrderDto order(String id, long payout, int weight, int volume, boolean hazmat) {
        OrderDto o = new OrderDto();
        o.setId(id);
        o.setPayoutCents(payout);
        o.setWeightLbs(weight);
        o.setVolumeCuft(volume);
        o.setOrigin("Los Angeles, CA");
        o.setDestination("Dallas, TX");
        o.setPickupDate(LocalDate.of(2025, 12, 1));
        o.setDeliveryDate(LocalDate.of(2025, 12, 10));
        o.setHazmat(hazmat);
        return o;
    }
}
