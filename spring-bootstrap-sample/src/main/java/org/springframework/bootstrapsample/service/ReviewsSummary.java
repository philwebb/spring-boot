package org.springframework.bootstrapsample.service;

import org.springframework.bootstrapsample.domain.Rating;

public interface ReviewsSummary {

	public long getNumberOfReviewsWithRating(Rating rating);

}
