package com.mtnrs.revenuesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RevenueSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(RevenueSyncApplication.class, args);
	}

}
