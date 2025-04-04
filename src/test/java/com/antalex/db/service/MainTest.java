package com.antalex.db.service;

import com.antalex.db.BaseIntegrationTest;
import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.dao.domain.PaymentDomain;
import com.antalex.db.service.impl.generators.ClientGenerator;
import com.antalex.db.service.impl.generators.PaymentGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Основные тесты")
class MainTest extends BaseIntegrationTest {
    @Autowired
    private DomainEntityManager domainManager;

    @Autowired
    private PaymentGenerator generator;

    @Test
    @DisplayName("Проверка создания данных")
    void generateDataTest() {
        List<PaymentDomain> payments = generator.generate(10000);
        assertThat(payments)
                .isNotNull()
                .hasSize(10000);

        ClientCategoryDomain category =
                domainManager.find(ClientCategoryDomain.class, "${categoryCode}=?", "VIP");
        assertThat(category.description()).isEqualTo("VIP-клиент");
    }
}