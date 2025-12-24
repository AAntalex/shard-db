package com.antalex.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

/**
 * Конфигурация интеграционных тестов.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:13";
    private static final String ORACLE_DOCKER_IMAGE_NAME = "gvenzl/oracle-xe:slim";
    private static final String MYSQL_DOCKER_IMAGE_NAME = "mysql:8.0.36";

    /**
     * Запускаем контейнеры и задаем параметры в Environment, чтобы переопределить конфигурационные файлы.
     */
    @DynamicPropertySource
    static void startContainersAndSetEnvironmentProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> pgContainer = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                .withInitScript("db/initPG.sql")
                .withReuse(true)
                .withTmpFs(Map.of("/var/postgres/data", "rw"));
        pgContainer.start();

        registry.add("DATASOURCE_JDBC_URL_PG", pgContainer::getJdbcUrl);
        registry.add("DATASOURCE_JDBC_USR_PG", pgContainer::getUsername);
        registry.add("DATASOURCE_JDBC_PSW_PG", pgContainer::getPassword);

        OracleContainer oracleContainer = new OracleContainer(DockerImageName.parse(ORACLE_DOCKER_IMAGE_NAME))
                .withCopyToContainer(
                        MountableFile.forClasspathResource("db/initOra.sql"),
                        "/container-entrypoint-startdb.d/init.sql"
                );
        oracleContainer.start();

        registry.add("DATASOURCE_JDBC_URL_ORA", oracleContainer::getJdbcUrl);
        registry.add("DATASOURCE_JDBC_USR_ORA", oracleContainer::getUsername);
        registry.add("DATASOURCE_JDBC_PSW_ORA", oracleContainer::getPassword);

        MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse(MYSQL_DOCKER_IMAGE_NAME))
                .withPassword("test");
        mySQLContainer.start();

        registry.add("DATASOURCE_JDBC_URL_MYSQL", mySQLContainer::getJdbcUrl);
        registry.add("DATASOURCE_JDBC_USR_MYSQL", mySQLContainer::getUsername);
        registry.add("DATASOURCE_JDBC_PSW_MYSQL", mySQLContainer::getPassword);
    }
}
