package com.antalex.db.service;

import com.antalex.db.BaseIntegrationTest;
import com.antalex.db.dao.criteria.ExternalPaymentCriteria$RepositoryImpl3;
import com.antalex.db.model.criteria.CriteriaPart;
import com.antalex.db.service.impl.generators.PaymentGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;


@DisplayName("Тесты работы с представлениями")
class CriteriaTest extends BaseIntegrationTest {
    @Autowired
    private DomainEntityManager domainManager;

    @Autowired
    private ShardEntityManager entityManager;

    @Autowired
    private ShardDataBaseManager dataBaseManager;

    @Autowired
    private PaymentGenerator paymentGenerator;


    @Test
    @DisplayName("Проверка работы с представлением")
    void criteriaTest() {
        ExternalPaymentCriteria$RepositoryImpl3 criteriaRepository =
                new ExternalPaymentCriteria$RepositoryImpl3(entityManager, dataBaseManager);
        Map<Long, CriteriaPart> criteriaPartMap = criteriaRepository.getCriteriaPartMap();

        System.out.println("SIZE = " + criteriaPartMap.size());
    }
}