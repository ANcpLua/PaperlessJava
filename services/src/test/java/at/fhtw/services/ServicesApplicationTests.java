package at.fhtw.services;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServicesApplicationTests {

    @Test
    @Order(1)
    void contextLoads() {
    }

    @Test
    @Order(2)
    void applicationStarts() {
        ServicesApplication.main(new String[]{});
    }

    @Test
    @Order(3)
    void applicationStartsWithCustomArgs() {
        ServicesApplication.main(new String[]{"--server.port=0"});
    }

    @Test
    @Order(4)
    void applicationConstructorCreatesInstance() {
        assertThat(new ServicesApplication()).isNotNull();
    }
}