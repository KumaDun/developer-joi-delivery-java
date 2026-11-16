package com.tw.joi.delivery.dto.response;

public record InventoryStockResponse(String outletId, String productId, int availableStock) {
}
