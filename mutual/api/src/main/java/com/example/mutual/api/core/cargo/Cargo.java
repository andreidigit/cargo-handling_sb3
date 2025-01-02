package com.example.mutual.api.core.cargo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class Cargo {
    private final int cargoId;
    private final String name;
    private final int weight;
    private Status status = Status.STOCK;
    private String serviceAddress = null;

    public enum Status {
        STOCK,
        WAIT,
        TRANSIT,
        DELIVERED
    }
}
