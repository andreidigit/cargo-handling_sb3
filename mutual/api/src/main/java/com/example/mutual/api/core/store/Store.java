package com.example.mutual.api.core.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Store {
    private int storeId = 0;
    private String location = null;
    private int capacity = 0;
    private int usedCapacity = 0;
    private String serviceAddress = null;
}
