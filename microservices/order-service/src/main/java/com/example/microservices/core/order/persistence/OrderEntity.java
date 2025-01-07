package com.example.microservices.core.order.persistence;

import com.example.mutual.api.core.order.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "order_unique_idx", unique = true, columnList = "orderId")
        }
)
public class OrderEntity {
    @Id
    @GeneratedValue
    private int id;
    @Version
    private int version;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    private int orderId;
    private int cargoId;
    private int fromStoreId;
    private int toStoreId;
    @Enumerated(EnumType.STRING)
    private Order.Status status;

    public OrderEntity(int orderId, int cargoId, int fromStoreId, int toStoreId, Order.Status status) {
        this.orderId = orderId;
        this.cargoId = cargoId;
        this.fromStoreId = fromStoreId;
        this.toStoreId = toStoreId;
        this.status = status;
    }
}
