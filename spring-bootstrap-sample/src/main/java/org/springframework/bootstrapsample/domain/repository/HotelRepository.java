package org.springframework.bootstrapsample.domain.repository;

import org.springframework.bootstrapsample.domain.City;
import org.springframework.bootstrapsample.domain.Hotel;
import org.springframework.data.repository.Repository;

public interface HotelRepository extends Repository<Hotel, Long> {

	Hotel findByCityAndName(City city, String name);

}
