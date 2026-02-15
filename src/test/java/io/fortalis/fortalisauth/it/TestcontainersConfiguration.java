package io.fortalis.fortalisauth.it;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        try (PostgreSQLContainer postgresContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("fortalis_auth")
                .withUsername("fortalis")
                .withPassword("fortalis")) {
            return postgresContainer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
