package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Sürücünün özünə təhkim olunmuş reysi imtina edərkən göndərdiyi (məcburi
// olmayan) səbəb mətni — bax DriverController#rejectTrip,
// DriverTripService#rejectTrip.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripRejectRequest {
    String reason;
}
