package com.tw.joi.delivery.domain;

import java.util.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Outlet<P extends Product> {

    protected String name;

    protected String description;

    protected String outletId;

    public void addProduct(P product) {
    }

    public Optional<P> findProduct(String productId) {
        return Optional.empty();
    }}

