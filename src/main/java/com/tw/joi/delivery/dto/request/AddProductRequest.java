package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.SpiceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProductRequest {

    @NotBlank(message = "User ID must not be blank")
    private String userId;

    @NotBlank(message = "Outlet ID must not be blank")
    private String outletId;

    @NotBlank(message = "Product ID must not be blank")
    private String productId;

    @Positive(message = "Quantity must be greater than zero")
    private int quantity;

    private SpiceLevel spiceLevel;

    private Boolean utensilsNeeded;
}
