package com.example.microservices.core.route.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "route",
        indexes = {
                @Index(name = "route_unique_idx", unique = true, columnList = "routeId"),
                @Index(name = "from_to_idx", columnList = "fromStoreId, toStoreId")
        }
)
public class RouteEntity implements Serializable {
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

    private int routeId;
    private String pathFromTo;
    private int fromStoreId;
    private int toStoreId;
    private int distanceFromTo;
    private int minutesFromTo;

    public RouteEntity(int routeId, int fromStoreId, int toStoreId) {
        this.routeId = routeId;
        this.fromStoreId = fromStoreId;
        this.toStoreId = toStoreId;
    }
}
