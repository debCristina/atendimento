package com.sea.atendimento;

import org.springframework.boot.SpringApplication;

public class TestAtendimentoApplication {

	public static void main(String[] args) {
		SpringApplication.from(AtendimentoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
