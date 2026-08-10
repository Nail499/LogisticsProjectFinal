package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçerin seçdiyi yük(lər) üçün "ən uyğun sürücü" sıralaması (bax
// service/DriverSuggestionService, CargoQueue.jsx). Real TMS platformalarında
// (Motive/Samsara) dispatch axınının təklif etdiyi sıralamanın analoqu —
// amma "qara qutu" olmasın deyə hər komponentin (məsafə/HOS/tutum/reytinq)
// ayrı-ayrı görünməsi üçün bütün xam dəyərlər də saxlanılır, dispetçer
// sadəcə ümumi bal-a etibar etmək məcburiyyətində deyil.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverSuggestionResponse {
    Long id;
    String fullName;
    String phone;

    boolean hasActiveTrip;
    String hosStatus;
    double remainingDrivingHours;
    boolean fatigueWarning;
    boolean hasUnresolvedDvirDefect;
    Double ratingAverage;
    Integer ratingCount;

    // null = məsafə bilinmir (sürücünün aktiv reysi/GPS ping-i yoxdur).
    Double distanceKm;
    // null = tətbiq olunmur (sürücünün öz qoşqusu yoxdur — şirkət avadanlığı
    // ilə işləyəcək, tutum reys yaradanda ayrıca yoxlanılacaq).
    Boolean capacitySufficient;

    // 0-100 aralığında ümumi uyğunluq balı (yüksək = daha uyğun) — bax
    // DriverSuggestionService#computeScore. Sırf təklifdir, məcburi deyil.
    double score;
}
