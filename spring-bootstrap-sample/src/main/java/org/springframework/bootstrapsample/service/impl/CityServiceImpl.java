package org.springframework.bootstrapsample.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrapsample.domain.City;
import org.springframework.bootstrapsample.domain.HotelSummary;
import org.springframework.bootstrapsample.domain.repository.CityRepository;
import org.springframework.bootstrapsample.domain.repository.HotelSummaryRepository;
import org.springframework.bootstrapsample.service.CitySearchCriteria;
import org.springframework.bootstrapsample.service.CityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component("cityService")
@Transactional
public class CityServiceImpl implements CityService {

	// FIXME deal with null repository return values

	private CityRepository cityRepository;

	private HotelSummaryRepository hotelSummaryRepository;

	public Page<City> findCities(CitySearchCriteria criteria, Pageable pageable) {
		Assert.notNull(criteria, "Criteria must not be null");
		String name = criteria.getName();
		if (!StringUtils.hasLength(name)) {
			return this.cityRepository.findAll(null);
		}
		String country = "";
		int splitPos = name.lastIndexOf(",");
		if (splitPos >= 0) {
			country = name.substring(splitPos + 1);
			name = name.substring(0, splitPos);
		}
		name = "%" + name.trim() + "%";
		country = "%" + country.trim() + "%";
		return this.cityRepository.findByNameLikeAndCountryLikeAllIgnoringCase(name, country, pageable);
	}

	public City getCity(String name, String country) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(country, "Country must not be null");
		return this.cityRepository.findByNameAndCountryAllIgnoringCase(name, country);
	}

	public Page<HotelSummary> getHotels(City city, Pageable pageable) {
		Assert.notNull(city, "City must not be null");
		return this.hotelSummaryRepository.findByCity(city, pageable);
	}

	@Autowired
	public void setCityRepository(CityRepository ciryRepository) {
		this.cityRepository = ciryRepository;
	}

	@Autowired
	public void setHotelSummaryRepository(HotelSummaryRepository hotelSummaryRepository) {
		this.hotelSummaryRepository = hotelSummaryRepository;
	}
}
