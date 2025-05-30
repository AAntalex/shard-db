package com.antalex.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * Конфигурация интеграционных тестов.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:13";
    /**
     * Запускаем контейнеры и задаем параметры в Environment, чтобы переопределить конфигурационные файлы.
     */
    @DynamicPropertySource
    static void startContainersAndSetEnvironmentProperties(DynamicPropertyRegistry registry) {
        IntStream.rangeClosed(1, 3)
                .forEach(idx -> {
                    PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                            .withInitScript("db/createSchema.sql")
                            .withReuse(true)
                            .withTmpFs(Map.of("/var/postgres/data", "rw"));
                    container.start();

                    registry.add("DATASOURCE_JDBC_URL" + idx, container::getJdbcUrl);
                    registry.add("DATASOURCE_JDBC_USR" + idx, container::getUsername);
                    registry.add("DATASOURCE_JDBC_PSW" + idx, container::getPassword);
                });
    }
}
