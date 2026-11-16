package com.tw.joi.delivery.dto.response;

import com.tw.joi.delivery.domain.StockStatus;
import java.util.List;

public record InventoryHealthResponse(String storeId, String storeName, List<ProductHealth> products) {

    public InventoryHealthResponse {
        products = List.copyOf(products);
    }

    public record ProductHealth(
            String productId,
            String productName,
            int availableStock,
            int threshold,
            StockStatus status
    ) {
    }
}
