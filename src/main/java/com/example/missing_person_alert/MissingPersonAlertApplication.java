package com.example.missing_person_alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MissingPersonAlertApplication {
	private static final Logger logger = LoggerFactory.getLogger(MissingPersonAlertApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(MissingPersonAlertApplication.class);
		app.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
			logger.info("Application is shutting down gracefully");
		});
		app.run(args);
	}
}