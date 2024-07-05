package com.antalex.db.controller;

import com.antalex.db.service.ShardEntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final ShardEntityManager entityManager;

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/a",
            produces = { "application/json" }
    )
    public ResponseEntity<Void> test() {
        System.out.println("AAA ");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}