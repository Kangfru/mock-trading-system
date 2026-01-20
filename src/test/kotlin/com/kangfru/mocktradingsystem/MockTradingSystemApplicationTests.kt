package com.kangfru.mocktradingsystem

import com.kangfru.mocktradingsystem.TestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestConfig::class)
class MockTradingSystemApplicationTests {

    @Test
    fun contextLoads() {
    }

}
