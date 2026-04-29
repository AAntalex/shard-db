package com.antalex.db.controller;

import com.antalex.db.model.DynamicDataBaseInfo;
import com.antalex.db.service.ShardDataBaseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/db/info")
@RequiredArgsConstructor
public class DatabaseInfoController {
    private final ShardDataBaseManager dataBaseManager;

    @Operation(
            summary = "Обновить кэш."
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
    ResponseEntity<DynamicDataBaseInfo> showDBInfo(@PathVariable String cluster, @PathVariable Short shardId) {
        return ResponseEntity.ok(
                dataBaseManager.getShard(dataBaseManager.getCluster(cluster), shardId).getDynamicDataBaseInfo());
    };
}