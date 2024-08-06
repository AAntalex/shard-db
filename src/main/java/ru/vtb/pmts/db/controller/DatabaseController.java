package ru.vtb.pmts.db.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.service.api.RemoteDatabaseService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/db/request")
@RequiredArgsConstructor
public class DatabaseController {
    private final RemoteDatabaseService databaseService;

    @PostMapping(
            value = "/executeQuery",
            consumes = { "application/json" }
    )
    public ResponseEntity<String> executeQuery(@RequestBody QueryDto query) {
        try {
            return ResponseEntity.ok(databaseService.executeQuery(query));
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(databaseService.getResponseError(err.getLocalizedMessage()));
        }
    }

    @PostMapping(
            value = "/executeUpdate",
            consumes = { "application/json" }
    )
    public ResponseEntity<String> executeUpdate(@RequestBody QueryDto query) {
        try {
            return ResponseEntity.ok(databaseService.executeUpdate(query));
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(databaseService.getResponseError(err.getLocalizedMessage()));
        }
    }

    @PostMapping(
            value = "/executeBatch",
            consumes = { "application/json" }
    )
    public ResponseEntity<String> executeBatch(@RequestBody QueryDto query) {
        try {
            return ResponseEntity.ok(databaseService.executeBatch(query));
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(databaseService.getResponseError(err.getLocalizedMessage()));
        }
    }

    @PutMapping(
            value = "/commit"
    )
    public ResponseEntity<String> commit(
            @RequestParam(required = false) UUID clientUuid,
            @RequestParam UUID taskUuid,
            @RequestParam(defaultValue = "true") Boolean postponedCommit) {
        try {
            databaseService.commit(clientUuid, taskUuid, postponedCommit);
            return ResponseEntity.ok(StringUtils.EMPTY);
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err.getLocalizedMessage());
        }
    }

    @PutMapping(
            value = "/rollback"
    )
    public ResponseEntity<String> rollback(
            @RequestParam(required = false) UUID clientUuid,
            @RequestParam UUID taskUuid,
            @RequestParam(defaultValue = "true") Boolean postponedCommit) {
        try {
            databaseService.rollback(clientUuid, taskUuid, postponedCommit);
            return ResponseEntity.ok(StringUtils.EMPTY);
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err.getLocalizedMessage());
        }
    }
}