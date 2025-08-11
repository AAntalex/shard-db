package com.antalex.db.service;

import com.antalex.db.BaseIntegrationTest;
import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.dao.domain.PaymentDomain;
import com.antalex.db.dao.model.Contract;
import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.model.dto.AttributeHistory;
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
    private PaymentGenerator paymentGenerator;

    @Autowired
    private ClientGenerator clientGenerator;

    @Test
    @DisplayName("Проверка создания данных")
    void createDataTest() {
        // Генерируем и сохраняем в БД сущности Оплат с полями домена
        List<PaymentDomain> payments = paymentGenerator.generate(10000);
        assertThat(payments)
                .isNotNull()
                .hasSize(10000);
        // Поиск всех доменов Оплат в БД
        payments = domainManager.findAll(PaymentDomain.class);
        assertThat(payments.size()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Проверка чтения данных")
    void findDataTest() {
        // Генерируем и сохраняем в БД сущности Оплат с полями домена
        List<PaymentDomain> payments = paymentGenerator.generate(10000);
        // Поиск домена Категории клиента в БД по условию
        ClientCategoryDomain category =
                domainManager.find(ClientCategoryDomain.class, "${categoryCode}=?", "VIP");
        assertThat(category.description()).isEqualTo("VIP-клиент");
        // Поиск всех доменов Оплат в БД по списку идентификаторов
        List<Long> ids = payments.stream().map(Domain::getId).toList();
        List<PaymentDomain> filteredPayments =
                domainManager.findAllByIds(PaymentDomain.class, ids);
        assertThat(filteredPayments.size()).isEqualTo(payments.size());
        // Поиск всех доменов Оплат в БД по списку идентификаторов и условию
        filteredPayments =
                domainManager.findAllByIds(PaymentDomain.class, "${num} < ?", ids, 100);
        assertThat(filteredPayments.size())
                .isEqualTo(
                        payments
                                .stream()
                                .filter(it -> it.num() < 100)
                                .count()
                );
    }

    @Test
    @DisplayName("Проверка работы с историей изменений реквизитов")
    void historyAttributeTest() {
        // Генерируем и сохраняем в БД сущность Клиент с полями домена
        clientGenerator.generate(1);
        // Поиск домена Клиент в БД по условию
        ClientDomain client =
                domainManager.find(ClientDomain.class, "${name}=?", "CLIENT1");
        // Изменение атрибутов Клиента по которым предусмотрено хранение истории изменений
        client
                .category(client.category()) /* атрибут сущности с типом ссылки */
                .contract() /* атрибут домена с типом произвольного класса */
                .description("newContract")
                .additions()
                .add("Addition4");

        // История изменений атрибута с типом ссылки
        List<AttributeHistory> history = domainManager.getAttributeHistory(client, "category");
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(1).value() instanceof Long).isTrue();

        // История изменений атрибута с типом произвольного класса
        history = domainManager.getAttributeHistory(client, "contract");
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).value() instanceof Contract).isTrue();
        assertThat(((Contract) history.get(0).value()).additions().size()).isEqualTo(3);
        assertThat(((Contract) history.get(1).value()).additions().size()).isEqualTo(4);

        // Сохраняем в БД историю последних изменений
        domainManager.update(client);
        history = domainManager.getAttributeHistory(client, "contract");
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).value() instanceof Contract).isTrue();
        assertThat(((Contract) history.get(0).value()).additions().size()).isEqualTo(3);
        assertThat(((Contract) history.get(1).value()).additions().size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Проверка работы с представлением")
    void criteriaTest() {

    }
}