package com.tw.joi.delivery.config;

import com.tw.joi.delivery.repository.*;
import com.tw.joi.delivery.seedData.SeedData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public StoreRepository storeRepository() {
        return new InMemoryOutletRepository(SeedData.outlets);
    }

    @Bean
    public UserRepository userRepository() {
        return new InMemoryUserRepository(SeedData.users);
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
