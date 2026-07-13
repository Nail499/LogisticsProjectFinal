package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverTripService {

    final TripRepository tripRepository;
    final CargoRepository cargoRepository;

    @Transactional
    public Trip updateStatus(Long tripId, Long driverId, TripStatus newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }

        validateTransition(trip.getStatus(), newStatus);

        trip.setStatus(newStatus);

        if (newStatus == TripStatus.PICKED_UP) {
            trip.setStartedAt(LocalDateTime.now());
        }
        if (newStatus == TripStatus.DELIVERED) {
            trip.setDeliveredAt(LocalDateTime.now());
        }

        tripRepository.save(trip);

        List<Cargo> cargos = cargoRepository.findByTripId(tripId);
        CargoStatus cargoStatus = mapCargoStatus(newStatus);
        if (cargoStatus != null) {
            for (Cargo cargo : cargos) {
                cargo.setStatus(cargoStatus);
                cargoRepository.save(cargo);
            }
        }

        return trip;
    }

    private void validateTransition(TripStatus current, TripStatus next) {
        boolean valid =
                (current == TripStatus.PLANNED && next == TripStatus.PICKED_UP) ||
                        (current == TripStatus.PICKED_UP && next == TripStatus.IN_TRANSIT) ||
                        (current == TripStatus.IN_TRANSIT && next == TripStatus.DELIVERED);

        if (!valid) {
            throw new RuntimeException("Status keçidi düzgün deyil: " + current + " -> " + next);
        }
    }

    private CargoStatus mapCargoStatus(TripStatus tripStatus) {
        return switch (tripStatus) {
            case IN_TRANSIT -> CargoStatus.IN_TRANSIT;
            case DELIVERED -> CargoStatus.DELIVERED;
            default -> null;
        };
    }
}