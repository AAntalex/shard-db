package ru.vtb.pmts.db.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.vtb.pmts.db.model.dto.IndexDto;
import ru.vtb.pmts.db.service.ShardEntityManager;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final ThreadLocal<String> test = new ThreadLocal<>();

    private final ShardEntityManager entityManager;

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/a",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<String> test(@RequestBody IndexDto testIdx) {
        log.info("AAA test = " + test.get());
        log.info("AAA tesIdx = " + testIdx);
        log.info("AAA Thread.currentThread().getId() = " + Thread.currentThread().getId());

        try {
            log.info("AAA dataBaseManager = " + entityManager);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }

        test.set("AAA");
        return ResponseEntity.ok(entityManager.toString());
    }
}