package com.tw.joi.delivery.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Product {

    protected String productId;
    protected String productName;
    protected BigDecimal mrp;
    protected BigDecimal unitPrice;
    protected int availableStock;
    @JsonIgnore
    protected Outlet<?> outlet;
}
