package com.antalex.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Конфигурация интеграционных тестов.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:13";
    private static final String DATABASE_NAME = "test";

    protected static final PostgreSQLContainer<?> POSTGRES_SQL =
            new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                    .withDatabaseName(DATABASE_NAME)
                    .withInitScript("db/createSchema.sql")
                    .withReuse(true)
                    .withTmpFs(Map.of("/var/postgres/data", "rw"));

    /**
     * Запускаем контейнеры и задаем параметры в Environment, чтобы переопределить конфигурационные файлы.
     */
    @DynamicPropertySource
    static void startContainersAndSetEnvironmentProperties(DynamicPropertyRegistry registry) {
        POSTGRES_SQL.start();

        registry.add("spring.datasource.default.url", POSTGRES_SQL::getJdbcUrl);
        registry.add("spring.datasource.default.username", POSTGRES_SQL::getUsername);
        registry.add("spring.datasource.default.password", POSTGRES_SQL::getPassword);
    }
}
