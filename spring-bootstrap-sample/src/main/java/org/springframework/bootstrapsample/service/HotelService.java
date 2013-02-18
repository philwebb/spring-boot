package org.springframework.bootstrapsample.service;

import org.springframework.bootstrapsample.domain.City;
import org.springframework.bootstrapsample.domain.Hotel;
import org.springframework.bootstrapsample.domain.Review;
import org.springframework.bootstrapsample.domain.ReviewDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

	Hotel getHotel(City city, String name);

	Page<Review> getReviews(Hotel hotel, Pageable pageable);

	Review getReview(Hotel hotel, int index);

	Review addReview(Hotel hotel, ReviewDetails details);

	ReviewsSummary getReviewSummary(Hotel hotel);

}
