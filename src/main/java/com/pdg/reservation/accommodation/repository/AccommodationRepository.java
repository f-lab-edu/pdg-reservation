package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.entity.Accommodation;
import org.springframework.data.repository.CrudRepository;

public interface AccommodationRepository extends CrudRepository<Accommodation, Long>, AccommodationRepositoryCustom {


}
