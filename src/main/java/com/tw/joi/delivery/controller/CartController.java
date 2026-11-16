package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.CartItem;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.request.UpdateCartItemQuantityRequest;
import com.tw.joi.delivery.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/product")
    public ResponseEntity<CartItem> addProductToCart(@Valid @RequestBody AddProductRequest addProductRequest) {
        return ResponseEntity.ok(cartService.addProductToCartForUser(addProductRequest));
    }

    @GetMapping("/view")
    public ResponseEntity<Cart> viewCart(@RequestParam(name = "userId") @NotBlank String userId) {
        return ResponseEntity.ok(cartService.getCartSnapshotForUser(userId));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable("cartItemId") @NotBlank String cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(
                request.userId(), cartItemId, request.quantity()));
    }
}
