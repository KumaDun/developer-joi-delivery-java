package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.dto.response.LastPurchaseResponse;
import com.tw.joi.delivery.service.LastPurchaseService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final LastPurchaseService lastPurchaseService;

    @GetMapping("/{userId}/last-purchase")
    public ResponseEntity<LastPurchaseResponse> getLastPurchase(
            @PathVariable("userId") @NotBlank String userId) {
        return lastPurchaseService.getLastPurchase(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
