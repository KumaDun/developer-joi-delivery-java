package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.Outlet;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.response.LastPurchaseResponse;
import com.tw.joi.delivery.repository.OrderRepository;
import com.tw.joi.delivery.repository.StoreRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LastPurchaseService {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public Optional<LastPurchaseResponse> getLastPurchase(String userId) {
        User user = userService.fetchUserById(userId);
        String orderId;
        synchronized (user) {
            orderId = user.getLastOrderId();
        }
        if (orderId == null) {
            return Optional.empty();
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Last purchase references a missing order"));
        if (!userId.equals(order.getUserId())) {
            throw new IllegalStateException("Last purchase belongs to another user");
        }

        String outletName = storeRepository.findById(order.getOutletId())
                .map(Outlet::getName)
                .orElse(null);

        return Optional.of(new LastPurchaseResponse(
                order.getOrderId(), order.getOutletId(), outletName, order.getCreatedAt()));
    }
}
