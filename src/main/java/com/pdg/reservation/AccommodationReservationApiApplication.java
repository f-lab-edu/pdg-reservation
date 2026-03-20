package com.pdg.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableAsync       // @Async 활성화
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableJpaAuditing // [필수] JPA Auditing 기능 활성화
@SpringBootApplication(scanBasePackages = "com.pdg") // com.pdg 하위의 모든 패키지를 스캔
public class AccommodationReservationApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccommodationReservationApiApplication.class, args);
	}

}
