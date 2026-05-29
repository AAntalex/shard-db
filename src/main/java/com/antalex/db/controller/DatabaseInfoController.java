package com.antalex.db.controller;

import com.antalex.db.model.dto.response.ResponseClusterInfoDto;
import com.antalex.db.model.dto.response.ResponseInstanceInfoDto;
import com.antalex.db.service.api.DatabaseInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/db/info")
@RequiredArgsConstructor
public class DatabaseInfoController {
    private final DatabaseInfoService databaseInfoService;

    @Operation(
            summary = "Информация об инстансе БД."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Content"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error"),
            @ApiResponse(responseCode = "502", description = "Bad Gateway"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable")
    })
    @GetMapping("/showDBInfo/{cluster}/{shardId}")
    ResponseEntity<ResponseInstanceInfoDto> showDBInfo(@PathVariable String cluster, @PathVariable Short shardId) {
        return ResponseEntity.ok(databaseInfoService.getDatabaseInfo(cluster, shardId));
    }

    @Operation(
            summary = "Информация об кластере БД."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Content"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error"),
            @ApiResponse(responseCode = "502", description = "Bad Gateway"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable")
    })
    @GetMapping("/showDBInfo/{cluster}")
    ResponseEntity<ResponseClusterInfoDto> showDBInfo(@PathVariable String cluster) {
        return ResponseEntity.ok(databaseInfoService.getDatabaseInfo(cluster));
    }

    @Operation(
            summary = "Информация о всех кластереах БД."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Content"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error"),
            @ApiResponse(responseCode = "502", description = "Bad Gateway"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable")
    })
    @GetMapping("/showDBInfo")
    ResponseEntity<Map<String, ResponseClusterInfoDto>> showDBInfo() {
        return ResponseEntity.ok(databaseInfoService.getDatabaseInfo());
    }

}