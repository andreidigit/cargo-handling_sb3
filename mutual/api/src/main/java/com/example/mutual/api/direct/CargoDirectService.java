package com.example.mutual.api.direct;

import com.example.mutual.api.core.cargo.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "Cargo", description = "REST API for cargo information.")
@RequestMapping("/api/v1/cargoes")
public interface CargoDirectService {

    @Operation(
            summary = "${api.cargo.get-cargo.description}",
            description = "${api.cargo.get-cargo.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @GetMapping(
            value = "/cargo/{cargoId}",
            produces = "application/json")
    Mono<Cargo> getCargo(@PathVariable int cargoId);

    @Operation(
            summary = "${api.cargo.create-cargo.description}",
            description = "${api.cargo.create-cargo.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/cargo",
            consumes = "application/json")
    Mono<Void> createCargo(@RequestBody Cargo body);

    @Operation(
            summary = "${api.cargo.update-cargo.description}",
            description = "${api.cargo.update-cargo.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/cargo/update",
            consumes = "application/json")
    Mono<Void> updateCargo(@RequestBody Cargo body);

    @Operation(
            summary = "${api.cargo.delete-cargo.description}",
            description = "${api.cargo.delete-cargo.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/cargo/{cargoId}")
    Mono<Void> deleteCargo(@PathVariable int cargoId);
}
