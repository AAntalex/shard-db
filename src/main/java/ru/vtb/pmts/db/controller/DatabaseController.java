package ru.vtb.pmts.db.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.RemoteButchResultDto;
import ru.vtb.pmts.db.service.api.RemoteDatabaseService;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/db/request")
@RequiredArgsConstructor
public class DatabaseController {
    private final RemoteDatabaseService databaseService;

    @PostMapping(
            value = "/executeBatch",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<RemoteButchResultDto> executeBatch(@RequestBody QueryDto query) {
        return ResponseEntity.ok(databaseService.executeBatch(query));
    }
}