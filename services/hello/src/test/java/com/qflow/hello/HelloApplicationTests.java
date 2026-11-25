package com.qflow.hello;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {"spring.flyway.enabled=false", "spring.data.jdbc.repositories.enabled=false"})
class HelloApplicationTests {

    @Test
    void contextLoads() {}
}
