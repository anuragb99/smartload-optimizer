package com.smartload.optimizer.controller;

import com.smartload.optimizer.model.dto.OptimizeRequest;
import com.smartload.optimizer.model.dto.OptimizeResponse;
import com.smartload.optimizer.service.LoadOptimizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/load-optimizer")
@RequiredArgsConstructor
public class LoadOptimizerController {

    private final LoadOptimizerService loadService;

    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimize(@Valid @RequestBody OptimizeRequest request) {
        OptimizeResponse response = loadService.optimize(request);
        return ResponseEntity.ok(response);
    }
}
