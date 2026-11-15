package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.exception.ProductDoesNotExistException;
import com.tw.joi.delivery.exception.StoreIdDoesNotExistException;
import com.tw.joi.delivery.repository.StoreRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

//    private final List<GroceryProduct> products= SeedData.groceryProducts;

    private final StoreRepository storeRepository;

    public ProductService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Product getProduct(String productId, String outletId) {
        Outlet<?> store = storeRepository.findById(outletId)
                .orElseThrow(() ->
                        new StoreIdDoesNotExistException(outletId));

        return store.findProduct(productId)
                .orElseThrow(() ->
                        new ProductDoesNotExistException(productId, outletId));
    }

    public Product getProduct(String productId) {
        return storeRepository.findAll().stream()
                .flatMap(store -> store.findProduct(productId).stream())
                .findFirst()
                .orElseThrow(() -> new ProductDoesNotExistException(productId));
    }

}
