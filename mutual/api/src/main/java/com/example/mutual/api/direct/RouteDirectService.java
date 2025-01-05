package com.example.mutual.api.direct;

import com.example.mutual.api.core.route.Route;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "Route", description = "REST API for route information.")
@RequestMapping("/api/v1/routes")
public interface RouteDirectService {

    @Operation(
            summary = "${api.route.get-route.description}",
            description = "${api.route.get-route.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @GetMapping(
            value = "/route/{routeId}",
            produces = "application/json")
    Mono<Route> getRoute(@PathVariable int routeId);

    @Operation(
            summary = "${api.route.create-route.description}",
            description = "${api.route.create-route.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/route",
            consumes = "application/json")
    Mono<Void> createRoute(@RequestBody Route body);

    @Operation(
            summary = "${api.route.update-route.description}",
            description = "${api.route.update-route.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/route/update",
            consumes = "application/json")
    Mono<Void> updateRoute(@RequestBody Route body);

    @Operation(
            summary = "${api.route.delete-route.description}",
            description = "${api.route.delete-route.notes}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
            @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/route/{routeId}")
    Mono<Void> deleteRoute(@PathVariable int routeId);
}
