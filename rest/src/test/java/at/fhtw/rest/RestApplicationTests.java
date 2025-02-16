package at.fhtw.rest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@SpringBootTest
class RestApplicationTests {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void mainCallsSpringApplicationRunWithEmptyArgs() {
        String[] args = {};
        try (MockedStatic<SpringApplication> mockedStatic = mockStatic(SpringApplication.class)) {
            RestApplication.main(args);
            mockedStatic.verify(() -> SpringApplication.run(RestApplication.class, args), times(1));
        }
    }

    @Test
    void mainCallsSpringApplicationRunWithCustomArgs() {
        String[] args = {"--server.port=8081", "--spring.profiles.active=test"};
        try (MockedStatic<SpringApplication> mockedStatic = mockStatic(SpringApplication.class)) {
            RestApplication.main(args);
            mockedStatic.verify(() -> SpringApplication.run(RestApplication.class, args), times(1));
        }
    }

    @Test
    void contextLoads() {
    }
}