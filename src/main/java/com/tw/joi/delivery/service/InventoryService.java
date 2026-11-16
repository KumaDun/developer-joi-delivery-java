package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.dto.response.InventoryStockResponse;
import com.tw.joi.delivery.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductService productService;

    public InventoryStockResponse setStock(String outletId, String productId, int availableStock) {
        validateIds(outletId, productId);
        Assert.isTrue(availableStock >= 0, "Available stock must not be negative");
        Product product = productService.getProduct(productId, outletId);

        synchronized (product) {
            product.setAvailableStock(availableStock);
            return new InventoryStockResponse(outletId, product.getProductId(), availableStock);
        }
    }

    public InventoryStockResponse adjustStock(String outletId, String productId, int delta) {
        validateIds(outletId, productId);
        Assert.isTrue(delta != 0, "Stock adjustment must not be zero");
        Product product = productService.getProduct(productId, outletId);

        synchronized (product) {
            int currentStock = validateCurrentStock(product);
            long adjustedStock = (long) currentStock + delta;
            if (adjustedStock < 0) {
                throw new InsufficientStockException(productId, -(long) delta, currentStock);
            }
            Assert.isTrue(adjustedStock <= Integer.MAX_VALUE, "Stock exceeds supported maximum");

            int availableStock = (int) adjustedStock;
            product.setAvailableStock(availableStock);
            return new InventoryStockResponse(outletId, product.getProductId(), availableStock);
        }
    }

    private void validateIds(String outletId, String productId) {
        Assert.hasText(outletId, "Outlet ID must not be blank");
        Assert.hasText(productId, "Product ID must not be blank");
    }

    private int validateCurrentStock(Product product) {
        int currentStock = product.getAvailableStock();
        if (currentStock < 0) {
            throw new IllegalStateException("Invalid inventory values for product: " + product.getProductId());
        }
        return currentStock;
    }
}
