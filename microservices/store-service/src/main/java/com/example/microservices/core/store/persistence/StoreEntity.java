package com.example.microservices.core.store.persistence;

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
        name = "stores",
        indexes = {
                @Index(name = "store_unique_idx", unique = true, columnList = "storeId")
        }
)
public class StoreEntity {
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

    private int storeId;
    private String location;
    private int capacity;
    private int usedCapacity;

    public StoreEntity(int storeId, String location, int capacity, int usedCapacity) {
        this.storeId = storeId;
        this.location = location;
        this.capacity = capacity;
        this.usedCapacity = usedCapacity;
    }
}
