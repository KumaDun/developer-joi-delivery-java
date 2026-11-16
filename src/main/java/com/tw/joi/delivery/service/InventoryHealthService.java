package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.StockStatus;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse.ProductHealth;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.StoreRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class InventoryHealthService {

    private final StoreRepository storeRepository;

    public InventoryHealthResponse getHealth(String storeId) {
        Assert.hasText(storeId, "Store ID must not be blank");
        Outlet<?> outlet = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreIdDoesNotExistException(storeId));

        if (!(outlet instanceof GroceryStore store)) {
            throw new IllegalArgumentException("Inventory health is only available for grocery stores");
        }

        List<ProductHealth> products = store.getInventory().stream()
                .map(this::getProductHealth)
                .sorted(Comparator.comparing(ProductHealth::productId))
                .toList();

        return new InventoryHealthResponse(store.getOutletId(), store.getName(), products);
    }

    private ProductHealth getProductHealth(GroceryProduct product) {
        synchronized (product) {
            return readProductHealth(product);
        }
    }

    private ProductHealth readProductHealth(GroceryProduct product) {
        String productId = product.getProductId();
        String productName = product.getProductName();
        int stock = product.getAvailableStock();
        int threshold = product.getThreshold();

        if (stock < 0 || threshold < 0) {
            throw new IllegalStateException("Invalid inventory values for product: " + productId);
        }

        StockStatus status = stock == 0 ? StockStatus.OUT_OF_STOCK
                : stock <= threshold ? StockStatus.LOW_STOCK : StockStatus.HEALTHY;

        return new ProductHealth(productId, productName, stock, threshold, status);
    }
}
