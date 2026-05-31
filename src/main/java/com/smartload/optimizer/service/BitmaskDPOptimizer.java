package com.smartload.optimizer.service;

import com.smartload.optimizer.model.OptimizationResult;
import com.smartload.optimizer.model.dto.OrderDto;
import com.smartload.optimizer.model.dto.TruckDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Bitmask based optimizer used to find the best combination of orders
 * that can fit into a truck while maximizing the payout.
 *
 * How it works:
 * 1. Each order is represented by a bit in a mask.
 *    If a bit is set, that order is considered selected.
 *
 * 2. We iterate through all possible combinations (2^n subsets)
 *    and calculate the total payout, weight and volume for each subset.
 *
 * 3. Any combination that exceeds truck capacity or violates
 *    route/hazmat compatibility rules is skipped.
 *
 * 4. Among all valid combinations, we keep track of the one
 *    with the highest payout and return it as the result.
 *
 * Performance:
 * - Time Complexity: O(2^n)
 * - Space Complexity: O(2^n)
 *
 * This approach works well for the given constraint of up to 22 orders.
 *
 * Compatibility Rules:
 * - All selected orders must have the same origin and destination.
 * - Hazmat and non-hazmat orders cannot be mixed together.
 * - pickup_date <= delivery_date is validated before reaching
 *   the optimizer as per the assignment requirements.
 */
@Component
public class BitmaskDPOptimizer implements LoadOptimizer {

    @Override
    public OptimizationResult optimize(TruckDto truck, List<OrderDto> orders) {
        int n = orders.size();
        if (n == 0) {
            return emptyResult();
        }

        // Pre-compute per-order arrays for cache-friendly inner loop access
        long[] payout = new long[n];
        int[]  weight = new int[n];
        int[]  volume = new int[n];
        boolean[] hazmat = new boolean[n];
        String[] origin      = new String[n];
        String[] destination = new String[n];

        for (int i = 0; i < n; i++) {
            OrderDto o = orders.get(i);
            payout[i]      = o.getPayoutCents();
            weight[i]      = o.getWeightLbs();
            volume[i]      = o.getVolumeCuft();
            hazmat[i]      = o.isHazmat();
            origin[i]      = o.getOrigin().strip().toLowerCase();
            destination[i] = o.getDestination().strip().toLowerCase();
        }

        int maxWeight = truck.getMaxWeightLbs();
        int maxVolume = truck.getMaxVolumeCuft();
        int totalMasks = 1 << n;  // 2^n

        // dp[mask] = total payout for this subset (-1 = infeasible)
        long[] dp = new long[totalMasks];

        // We don't store intermediate infeasibility explicitly;
        // infeasible masks stay at 0 and are excluded from the best-payout check.

        // Track cumulative weight and volume per mask to avoid re-summing each time.
        // Build incrementally: dp[mask | (1<<i)] from dp[mask].
        int[] totalWeight = new int[totalMasks];
        int[] totalVolume = new int[totalMasks];

        long bestPayout   = 0;
        int  bestMask     = 0;   // 0 = no orders selected (valid empty result)

        for (int mask = 1; mask < totalMasks; mask++) {
            // Find the lowest set bit — this is the order we "added" to get this mask
            int i = Integer.numberOfTrailingZeros(mask);
            int prevMask = mask ^ (1 << i);

            int w = totalWeight[prevMask] + weight[i];
            int v = totalVolume[prevMask] + volume[i];

            // Hard capacity check
            if (w > maxWeight || v > maxVolume) {
                // Mark infeasible with sentinel (reuse 0; payout stays 0)
                totalWeight[mask] = Integer.MAX_VALUE; // sentinel: skip route/hazmat check
                totalVolume[mask] = Integer.MAX_VALUE;
                continue;
            }

            // Skip if previous mask was already infeasible
            if (totalWeight[prevMask] == Integer.MAX_VALUE) {
                totalWeight[mask] = Integer.MAX_VALUE;
                totalVolume[mask] = Integer.MAX_VALUE;
                continue;
            }

            // Route compatibility: all orders must share origin + destination
            // We check by comparing each order in the mask against order i
            if (!isRouteCompatible(mask, i, origin, destination)) {
                totalWeight[mask] = Integer.MAX_VALUE;
                totalVolume[mask] = Integer.MAX_VALUE;
                continue;
            }

            // Hazmat isolation: if ANY order is hazmat, ALL must be hazmat
            if (!isHazmatCompatible(mask, n, hazmat)) {
                totalWeight[mask] = Integer.MAX_VALUE;
                totalVolume[mask] = Integer.MAX_VALUE;
                continue;
            }

            totalWeight[mask] = w;
            totalVolume[mask] = v;
            dp[mask] = dp[prevMask] + payout[i];

            if (dp[mask] > bestPayout) {
                bestPayout = dp[mask];
                bestMask   = mask;
            }
        }

        if (bestMask == 0) {
            return emptyResult();
        }

        // Reconstruct selected orders from bestMask
        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1 << i)) != 0) {
                selectedIds.add(orders.get(i).getId());
            }
        }

        return OptimizationResult.builder()
                .selectedOrderIds(selectedIds)
                .totalPayoutCents(bestPayout)
                .totalWeightLbs(totalWeight[bestMask])
                .totalVolumeCuft(totalVolume[bestMask])
                .build();
    }

    //Compatibility checks
    /**
     * All orders in the mask must share the same origin and destination as order {@code i}.
     * Since we build masks incrementally (prevMask → mask by adding bit i), we only need
     * to check the lowest bit of prevMask against order i — the chain ensures transitivity.
     */
    private boolean isRouteCompatible(int mask, int newBit, String[] origin, String[] destination) {
        int prevMask = mask ^ (1 << newBit);
        if (prevMask == 0) return true; // first order — always compatible with itself

        int firstBit = Integer.numberOfTrailingZeros(prevMask);
        return origin[firstBit].equals(origin[newBit])
                && destination[firstBit].equals(destination[newBit]);
    }

    /**
     * Hazmat isolation rule: the mask either contains ONLY hazmat orders or ONLY non-hazmat orders.
     * Checked when adding the new bit: if the existing set is hazmat-only and new is not (or vice versa),
     * the combination is invalid.
     */
    private boolean isHazmatCompatible(int mask, int n, boolean[] hazmat) {
        boolean hasHazmat    = false;
        boolean hasNonHazmat = false;
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) {
                if (hazmat[i]) hasHazmat    = true;
                else           hasNonHazmat = true;
                if (hasHazmat && hasNonHazmat) return false;
            }
        }
        return true;
    }

    private OptimizationResult emptyResult() {
        return OptimizationResult.builder()
                .selectedOrderIds(List.of())
                .totalPayoutCents(0L)
                .totalWeightLbs(0)
                .totalVolumeCuft(0)
                .build();
    }
}
