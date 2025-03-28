package com.antalex.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.vtb.pmts.dictionary.service.DictRegistryDataLoadingScheduler;

import java.util.Map;

/**
 * Конфигурация интеграционных тестов.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:13";
    private static final String DATABASE_NAME = "common_dictionary";

    protected static final PostgreSQLContainer<?> POSTGRES_SQL = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                    .withDatabaseName(DATABASE_NAME)
                    .withInitScript("db/createSchema.sql")
                    .withReuse(true)
                    .withTmpFs(Map.of("/var/postgres/data", "rw"));

    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse(REDIS_DOCKER_IMAGE_NAME))
            .withExposedPorts(REDIS_PORT);

    protected static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse(KAFKA_DOCKER_IMAGE_NAME));

    /**
     * Запускаем контейнеры и задаем параметры в Environment, чтобы переопределить конфигурационные файлы.
     */
    @DynamicPropertySource
    static void startContainersAndSetEnvironmentProperties(DynamicPropertyRegistry registry) {
        POSTGRES_SQL.start();
        REDIS.start();
        KAFKA.start();

        registry.add("spring.datasource.default.url", POSTGRES_SQL::getJdbcUrl);
        registry.add("spring.datasource.default.username", POSTGRES_SQL::getUsername);
        registry.add("spring.datasource.default.password", POSTGRES_SQL::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));

        registry.add("dictionary-registry.update.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("sutp-zod.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("legal-entity.product-profile.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("legal-entity.card.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
