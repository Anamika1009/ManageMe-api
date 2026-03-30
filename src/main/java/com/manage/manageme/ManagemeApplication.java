package com.manage.manageme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.manage.manageme.repository")
public class ManagemeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManagemeApplication.class, args);
	}

}
