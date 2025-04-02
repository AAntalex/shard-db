package com.antalex.db.service;

import com.antalex.db.BaseIntegrationTest;
import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.service.impl.generators.ClientCategoryGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@DisplayName("Основные тесты")
class MainTest extends BaseIntegrationTest {
    @Autowired
    private DomainEntityManager entityManager;

    @Autowired
    private ClientCategoryGenerator clientCategoryGenerator;

    @Test
    @DisplayName("Проверка создания данных")
    void generateDataTest() {
        List<ClientCategoryDomain> clientCategoryDomains = clientCategoryGenerator.generate();
        clientCategoryDomains = entityManager.findAll(ClientCategoryDomain.class);
    }
}