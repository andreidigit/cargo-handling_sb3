package com.example.microservices.core.cargo.persistence;

import com.example.mutual.api.core.cargo.Cargo;
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
        name = "cargoes",
        indexes = {
                @Index(name = "cargo_unique_idx", unique = true, columnList = "cargoId")
        }
)
public class CargoEntity {
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

    private int cargoId;
    private String name;
    private int weight;
    @Enumerated(EnumType.STRING)
    private Cargo.Status status;

    public CargoEntity(int cargoId, String name, int weight, Cargo.Status status) {
        this.cargoId = cargoId;
        this.name = name;
        this.weight = weight;
        this.status = status;
    }
}
