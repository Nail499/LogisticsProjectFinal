package com.ltc.logisticsproject.dto.push;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Brauzerin PushSubscription.toJSON() formatına uyğun — bax
// utils/push.js#sendSubscriptionToServer. Sadə {endpoint, p256dh, auth}
// yerinə {endpoint, keys:{p256dh, auth}} olması brauzerin öz native JSON
// formatı olduğu üçündür, əlavə çevirmə lazım olmasın deyə.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSubscribeRequest {
    String endpoint;
    Keys keys;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Keys {
        String p256dh;
        String auth;
    }
}
