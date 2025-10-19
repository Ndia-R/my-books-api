package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.util.PageableUtils;

@Mapper(componentModel = "spring", uses = {BookMapper.class})
public interface ReviewMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "avatarPath", source = "user.avatarPath")
    @Mapping(target = "book", source = "book")
    ReviewResponse toReviewResponse(Review review);

    List<ReviewResponse> toReviewResponseList(List<Review> reviews);

    default PageResponse<ReviewResponse> toPageResponse(Page<Review> reviews) {
        List<ReviewResponse> responses = toReviewResponseList(reviews.getContent());
        return PageableUtils.toPageResponse(reviews, responses);
    }
}
