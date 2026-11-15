package com.tw.joi.delivery.config;

import com.tw.joi.delivery.repository.InMemoryGroceryStoreRepository;
import com.tw.joi.delivery.repository.InMemoryUserRepository;
import com.tw.joi.delivery.repository.StoreRepository;
import com.tw.joi.delivery.repository.UserRepository;
import com.tw.joi.delivery.seedData.SeedData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public StoreRepository storeRepository() {
        return new InMemoryGroceryStoreRepository(SeedData.groceryStores);
    }

    @Bean
    public UserRepository userRepository() {
        return new InMemoryUserRepository(SeedData.users);
    }
}