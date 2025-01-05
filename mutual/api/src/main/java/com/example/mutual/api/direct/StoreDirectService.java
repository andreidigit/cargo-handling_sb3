package com.example.mutual.api.direct;

import com.example.mutual.api.core.store.Store;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "Store", description = "REST API for store information.")
@RequestMapping("/api/v1/stores")
public interface StoreDirectService {

    @Operation(
            summary = "${api.store.get-store.description}",
            description = "${api.store.get-store.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @GetMapping(
            value = "/store/{storeId}",
            produces = "application/json")
    Mono<Store> getStore(@PathVariable int storeId);

    @Operation(
            summary = "${api.store.create-store.description}",
            description = "${api.store.create-store.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/store",
            consumes = "application/json")
    Mono<Void> createStore(@RequestBody Store body);

    @Operation(
            summary = "${api.store.update-store.description}",
            description = "${api.store.update-store.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/store/update",
            consumes = "application/json")
    Mono<Void> updateStore(@RequestBody Store body);

    @Operation(
            summary = "${api.store.delete-store.description}",
            description = "${api.store.delete-store.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/store/{storeId}")
    Mono<Void> deleteStore(@PathVariable int storeId);
}
