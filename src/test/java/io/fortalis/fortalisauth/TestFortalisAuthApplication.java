package io.fortalis.fortalisauth;

import org.springframework.boot.SpringApplication;

public class TestFortalisAuthApplication {

	public static void main(String[] args) {
		SpringApplication.from(FortalisAuthApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
