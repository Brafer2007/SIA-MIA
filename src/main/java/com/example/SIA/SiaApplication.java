package com.example.SIA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiaApplication {

	public static void main(String[] args) {
		System.out.println("🚀 Iniciando servidor SIA...");
		SpringApplication.run(SiaApplication.class, args);
	}
}