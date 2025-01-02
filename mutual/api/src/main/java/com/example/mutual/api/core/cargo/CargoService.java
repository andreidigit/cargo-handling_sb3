package com.example.mutual.api.core.cargo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public interface CargoService {

    /**
     * Sample usage: "curl $HOST:$PORT/cargo/1".
     *
     * @param cargoId Id of the cargo
     * @return the cargo, if found, else null
     */
    @GetMapping(value = "/cargo/{cargoId}", produces = "application/json")
    Mono<Cargo> getCargo(@PathVariable int cargoId);
    Mono<Cargo> createCargo(@RequestBody Cargo body);
    Mono<Void> deleteCargo(@PathVariable int cargoId);
}
