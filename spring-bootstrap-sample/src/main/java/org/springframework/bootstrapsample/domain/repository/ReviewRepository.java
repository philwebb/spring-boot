package org.springframework.bootstrapsample.domain.repository;

import org.springframework.bootstrapsample.domain.Hotel;
import org.springframework.bootstrapsample.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ReviewRepository extends Repository<Review, Long> {

	Page<Review> findByHotel(Hotel hotel, Pageable pageable);

	Review findByHotelAndIndex(Hotel hotel, int index);

}
