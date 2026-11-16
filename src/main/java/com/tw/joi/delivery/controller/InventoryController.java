package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.service.InventoryHealthService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryHealthService inventoryHealthService;

    @GetMapping("/health")
    public ResponseEntity<InventoryHealthResponse> fetchStoreInventoryHealth(
            @RequestParam(name = "storeId") @NotBlank String storeId) {
        return ResponseEntity.ok(inventoryHealthService.getHealth(storeId));
    }
}
