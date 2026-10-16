package com.rukamini.mail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class APIController {

    @GetMapping(path = "/health", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @CrossOrigin(origins = "*")
    @Operation(summary = "Health Check")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Mail Application Working"),
                @ApiResponse(responseCode = "404", description = "Application not found"),
                @ApiResponse(responseCode = "403", description = "Access Forbidden"),
                @ApiResponse(responseCode = "400", description = "Bad Request")
            })
    public String fetchIBXTeams() {
        return "Mail Application Working";
    }
}
