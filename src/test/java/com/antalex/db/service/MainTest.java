package com.antalex.db.service;

import com.antalex.db.BaseIntegrationTest;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.service.impl.generators.ClientGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Основные тесты")
class MainTest extends BaseIntegrationTest {
    @Autowired
    private DomainEntityManager entityManager;

    @Autowired
    private ClientGenerator clientGenerator;

    @Test
    @DisplayName("Проверка создания данных")
    void generateDataTest() {
        List<ClientDomain> clients = clientGenerator.generate(1000);
        assertThat(clients)
                .isNotNull()
                .hasSize(1000);
    }
}