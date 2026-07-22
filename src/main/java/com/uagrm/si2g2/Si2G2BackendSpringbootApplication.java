package com.uagrm.si2g2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class Si2G2BackendSpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(Si2G2BackendSpringbootApplication.class, args);
	}

}
