package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.CustomerSummary;
import com.ltc.logisticsproject.dto.LiveTripResponse;
import com.ltc.logisticsproject.dto.TrackingUpdateMessage;
import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.TrackingLog;
import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.repository.TrackingLogRepository;
import com.ltc.logisticsproject.repository.TripRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

// Stage 6 — fans a single trip's new GPS ping / status change out over the
// WebSocket broker (see WebSocketConfig) so the Dispatcher Control Tower map
// and any open customer live-tracking pages update instantly instead of
// waiting for their next poll. Called from DriverController right after a
// tracking log is saved or a trip status changes.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripBroadcastService {

    final TripRepository tripRepository;
    final CargoRepository cargoRepository;
    final TrackingLogRepository trackingLogRepository;
    final SimpMessagingTemplate messagingTemplate;
    final RouteEstimationService routeEstimationService;

    public void broadcastTripUpdate(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) return;

        Double lat = null, lng = null;
        String lastUpdated = null;
        List<TrackingLog> logs = trackingLogRepository.findByTripIdOrderByRecordedAtAsc(tripId);
        if (!logs.isEmpty()) {
            TrackingLog last = logs.get(logs.size() - 1);
            lat = last.getLatitude();
            lng = last.getLongitude();
            lastUpdated = last.getRecordedAt().toString();
        }

        List<Cargo> cargos = cargoRepository.findByTripId(tripId);
        Cargo firstCargo = cargos.isEmpty() ? null : cargos.get(0);

        String destinationAddress = null;
        Double destLat = null, destLng = null;
        if (firstCargo != null) {
            destinationAddress = firstCargo.getDestinationAddress();
            destLat = firstCargo.getDestinationLatitude();
            destLng = firstCargo.getDestinationLongitude();
            if (lat == null) {
                lat = firstCargo.getPickupLatitude();
                lng = firstCargo.getPickupLongitude();
            }
        }

        // 1) Dispatcher Control Tower — one enriched trip per message.
        List<CustomerSummary> customers = cargos.stream().map(CustomerSummary::from).toList();
        LiveTripResponse dispatcherPayload = LiveTripResponse.builder()
                .tripId(trip.getId())
                .status(trip.getStatus())
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getPhone() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .trailerPlate(trip.getTrailer() != null ? trip.getTrailer().getPlateNumber() : null)
                .lastLatitude(lat)
                .lastLongitude(lng)
                .lastUpdatedAt(lastUpdated)
                .destinationAddress(destinationAddress)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .routeInfo(trip.getRouteInfo())
                .estimatedDistanceKm(trip.getEstimatedDistanceKm())
                .estimatedCost(trip.getEstimatedCost())
                .customers(customers)
                .build();
        messagingTemplate.convertAndSend("/topic/dispatcher/live-trips", dispatcherPayload);

        // 2) Customer live-tracking — one lightweight message per cargo on this
        // trip (a trip can carry several tracking numbers).
        for (Cargo cargo : cargos) {
            Integer etaMinutes = routeEstimationService.estimateEtaMinutes(
                    lat, lng, cargo.getDestinationLatitude(), cargo.getDestinationLongitude());

            TrackingUpdateMessage trackingPayload = TrackingUpdateMessage.builder()
                    .trackingNumber(cargo.getTrackingNumber())
                    .status(cargo.getStatus())
                    .lastLatitude(lat)
                    .lastLongitude(lng)
                    .lastUpdatedAt(lastUpdated)
                    .estimatedEtaMinutes(etaMinutes)
                    .build();
            messagingTemplate.convertAndSend("/topic/tracking/" + cargo.getTrackingNumber(), trackingPayload);
        }
    }
}
