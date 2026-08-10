package com.ltc.logisticsproject.dto.rating;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingResponse {
    Long id;
    Long tripId;
    Integer stars;
    String comment;
    String createdAt;
}
