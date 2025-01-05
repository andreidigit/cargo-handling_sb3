package com.example.mutual.api.core.cargo;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Cargo {
    private int cargoId = 0;
    private String name = null;
    private int weight = 0;
    private Status status = Status.STOCK;
    private String serviceAddress = null;

    public enum Status {
        STOCK,
        WAIT,
        TRANSIT,
        DELIVERED
    }
}
