package com.pdg.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // [필수] JPA Auditing 기능 활성화
@SpringBootApplication
public class AccommodationReservationApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccommodationReservationApiApplication.class, args);
	}

}
