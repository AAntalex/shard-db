package ru.vtb.pmts.db.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.vtb.pmts.db.model.Cluster;
import ru.vtb.pmts.db.model.Shard;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.service.ShardDataBaseManager;
import ru.vtb.pmts.db.service.ShardEntityManager;
import ru.vtb.pmts.db.service.api.TransactionalQuery;
import ru.vtb.pmts.db.utils.ShardUtils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DatabaseController {
    private final Map<String, Connection> activeConnections = new HashMap<>();
    private final ShardDataBaseManager dataBaseManager;
    private final ShardEntityManager entityManager;

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/db/request",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<String> request(@RequestBody QueryDto query) {
        TransactionalQuery transactionalQuery = dataBaseManager.getTransactionalTask(
                dataBaseManager.getShard(
                        dataBaseManager.getCluster(query.clusterName()), query.shardId()
                )
        )
                .addQuery(query.query(), query.queryType());


        return ResponseEntity.ok(entityManager.toString());
    }
}