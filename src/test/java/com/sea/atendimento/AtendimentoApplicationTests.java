package com.sea.atendimento;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AtendimentoApplicationTests {

	@Test
	void contextLoads() {
	}

}
