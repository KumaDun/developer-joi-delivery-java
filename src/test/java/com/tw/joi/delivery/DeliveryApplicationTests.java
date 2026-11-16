package com.tw.joi.delivery;

import com.tw.joi.delivery.domain.FoodProduct;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DeliveryApplicationTests {

	@Autowired
	private ProductService productService;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldLoadRestaurantMealsAlongsideGroceries() {
		FoodProduct meal = assertInstanceOf(FoodProduct.class,
				productService.getProduct("meal101", "restaurant101"));

		assertEquals("Spice Kitchen", meal.getOutlet().getName());
		assertEquals(new BigDecimal("25.00"), meal.getUnitPrice());
		assertEquals(20, meal.getAvailableStock());
		assertInstanceOf(GroceryProduct.class,
				productService.getProduct("product101", "store101"));
	}

}
