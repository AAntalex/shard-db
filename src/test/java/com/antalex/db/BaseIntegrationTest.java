package com.antalex.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Конфигурация интеграционных тестов.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:13";
    private static final String ORACLE_DOCKER_IMAGE_NAME = "gvenzl/oracle-xe:slim";
    private static final String MYSQL_DOCKER_IMAGE_NAME = "mysql:8.0.36";

    private static final List<String> TEST_CONTAINERS = List.of(POSTGRES_DOCKER_IMAGE_NAME, ORACLE_DOCKER_IMAGE_NAME);

    private static final Integer CONTAINERS_COUNT = 3;

    /**
     * Запускаем контейнеры и задаем параметры в Environment, чтобы переопределить конфигурационные файлы.
     */
    @DynamicPropertySource
    static void startContainersAndSetEnvironmentProperties(DynamicPropertyRegistry registry) {
        IntStream
                .range(0, CONTAINERS_COUNT)
                .forEach(idx -> {
                    JdbcDatabaseContainer<?> container =
                            initContainer(TEST_CONTAINERS.get(idx % TEST_CONTAINERS.size()));
                    container.start();
                    registry.add("DATASOURCE_JDBC_URL" + idx, container::getJdbcUrl);
                    registry.add("DATASOURCE_JDBC_USR" + idx, container::getUsername);
                    registry.add("DATASOURCE_JDBC_PSW" + idx, container::getPassword);
                });
    }

    static JdbcDatabaseContainer<?> initContainer(String containerName) {
        return switch (containerName) {
            case POSTGRES_DOCKER_IMAGE_NAME -> new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                    .withInitScript("db/initPG.sql")
                    .withReuse(true)
                    .withTmpFs(Map.of("/var/postgres/data", "rw"));
            case ORACLE_DOCKER_IMAGE_NAME -> new OracleContainer(DockerImageName.parse(ORACLE_DOCKER_IMAGE_NAME))
                    .withCopyToContainer(
                            MountableFile.forClasspathResource("db/initOra.sql"),
                            "/container-entrypoint-startdb.d/init.sql"
                    );
            case MYSQL_DOCKER_IMAGE_NAME -> new MySQLContainer<>(DockerImageName.parse(MYSQL_DOCKER_IMAGE_NAME));
            default -> throw new IllegalArgumentException("Unknown container name: " + containerName);
        };
    }
}
