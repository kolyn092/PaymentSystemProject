package com.paymentsystemproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PaymentSystemProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentSystemProjectApplication.class, args);
	}

}
