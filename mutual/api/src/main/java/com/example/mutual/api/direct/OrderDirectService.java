package com.example.mutual.api.direct;

import com.example.mutual.api.core.order.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "Order", description = "REST API for order information.")
@RequestMapping("/api/v1/orders")
public interface OrderDirectService {

    @Operation(
            summary = "${api.order.get-order.description}",
            description = "${api.order.get-order.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @GetMapping(
            value = "/order/{orderId}",
            produces = "application/json")
    Mono<Order> getOrder(@PathVariable int orderId);

    @Operation(
            summary = "${api.order.create-order.description}",
            description = "${api.order.create-order.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/order",
            consumes = "application/json")
    Mono<Void> createOrder(@RequestBody Order body);

    @Operation(
            summary = "${api.order.update-order.description}",
            description = "${api.order.update-order.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/order/update",
            consumes = "application/json")
    Mono<Void> updateOrder(@RequestBody Order body);

    @Operation(
            summary = "${api.order.delete-order.description}",
            description = "${api.order.delete-order.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/order/{orderId}")
    Mono<Void> deleteOrder(@PathVariable int orderId);
}
