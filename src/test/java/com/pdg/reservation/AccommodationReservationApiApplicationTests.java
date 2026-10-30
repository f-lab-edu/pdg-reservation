package com.pdg.reservation;

import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(TestContainersConfig.class)
class AccommodationReservationApiApplicationTests {

	@Test
	void contextLoads() {

	}

}
