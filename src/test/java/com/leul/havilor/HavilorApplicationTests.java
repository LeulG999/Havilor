package com.leul.havilor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HavilorApplicationTests {

    @Test
    void contextLoads() {
    }

}
