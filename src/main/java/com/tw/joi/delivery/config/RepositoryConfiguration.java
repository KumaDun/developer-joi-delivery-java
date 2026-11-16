package com.tw.joi.delivery.config;

import com.tw.joi.delivery.repository.*;
import com.tw.joi.delivery.seedData.SeedData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public SeedData seedData() {
        return SeedData.create();
    }

    @Bean
    public StoreRepository storeRepository(SeedData seedData) {
        return new InMemoryOutletRepository(seedData.getOutlets());
    }

    @Bean
    public UserRepository userRepository(SeedData seedData) {
        return new InMemoryUserRepository(seedData.getUsers());
    }

    @Bean
    public CartRepository cartRepository() {
        return new InMemoryCartRepository();
    }

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }
}
