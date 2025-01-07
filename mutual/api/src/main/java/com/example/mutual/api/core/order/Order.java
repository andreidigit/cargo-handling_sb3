package com.example.mutual.api.core.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Order {
    private int orderId;
    private int cargoId;
    private int fromStoreId;
    private int toStoreId;
    private Order.Status status = Status.NEW;
    private String serviceAddress = null;

    public enum Status {
        NEW,
        TRANSIT,
        COMPLETED
    }
}
