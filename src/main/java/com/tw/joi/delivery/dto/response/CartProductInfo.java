package com.tw.joi.delivery.dto.response;

import com.tw.joi.delivery.dto.response.CartInfo.ProductInfo;
import java.math.BigDecimal;

public record CartProductInfo(
        CartInfo cart,
        ProductInfo product,
        BigDecimal sellingPrice
) {
}