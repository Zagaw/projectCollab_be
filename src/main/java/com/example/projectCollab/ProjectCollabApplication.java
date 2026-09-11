package com.example.projectCollab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectCollabApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectCollabApplication.class, args);
	}
}
