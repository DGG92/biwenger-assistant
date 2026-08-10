package com.artajerjes.biwengerassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BiwengerAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(BiwengerAssistantApplication.class, args);
	}

}
