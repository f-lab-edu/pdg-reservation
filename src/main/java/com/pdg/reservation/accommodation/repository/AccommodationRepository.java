package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccommodationRepository extends CrudRepository<Accommodation, Long>, AccommodationRepositoryCustom {

    @Query("select acc.id from Accommodation acc")
    List<Long> findAllIds();

}
