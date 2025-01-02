package com.example.mutual.api.core.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class Order {
    private final int orderId;
    private List<OrderRecord> items = new ArrayList<>();
    private String serviceAddress = null;

    public record OrderRecord(int cargoId, int fromStoreId, int toStoreId, Order.Status status) {
    }

    public enum Status {
        NEW,
        TRANSIT,
        COMPLETED
    }
}
