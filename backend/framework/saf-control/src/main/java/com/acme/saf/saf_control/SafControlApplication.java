package com.acme.saf.saf_control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SafControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafControlApplication.class, args);
	}

}
