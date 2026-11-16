package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryStockResponse;
import com.tw.joi.delivery.dto.request.AdjustStockRequest;
import com.tw.joi.delivery.dto.request.SetStockRequest;
import com.tw.joi.delivery.service.InventoryHealthService;
import com.tw.joi.delivery.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryHealthService inventoryHealthService;
    private final InventoryService inventoryService;

    @GetMapping("/health")
    public ResponseEntity<InventoryHealthResponse> fetchStoreInventoryHealth(
            @RequestParam(name = "storeId") @NotBlank String storeId) {
        return ResponseEntity.ok(inventoryHealthService.getHealth(storeId));
    }

    @PutMapping("/{outletId}/products/{productId}/stock")
    public ResponseEntity<InventoryStockResponse> setStock(
            @PathVariable("outletId") @NotBlank String outletId,
            @PathVariable("productId") @NotBlank String productId,
            @Valid @RequestBody SetStockRequest request) {
        return ResponseEntity.ok(inventoryService.setStock(outletId, productId, request.availableStock()));
    }

    @PostMapping("/{outletId}/products/{productId}/stock-adjustments")
    public ResponseEntity<InventoryStockResponse> adjustStock(
            @PathVariable("outletId") @NotBlank String outletId,
            @PathVariable("productId") @NotBlank String productId,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(outletId, productId, request.delta()));
    }
}
