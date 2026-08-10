package com.ltc.logisticsproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// AI dəstək chat-inin (bax AiChatService) real dataya girişi buradan keçir.
// Hər tool YALNIZ öz çağıran rolunun icazə verdiyi datanı görür — məs.
// müştəri roluyla list_my_orders çağırılanda yalnız o müştərinin (customerId)
// öz sifarişləri qaytarılır, sürücü roluyla list_my_trips yalnız o sürücünün
// (driverId) öz reyslərini görür. roleEntityId AiChatService-dən gəlir (bax
// SupportChatController#chat — user.getCustomerId()/getDriverId()).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiToolExecutor {

    final CargoRepository cargoRepository;
    final TripRepository tripRepository;
    final FatigueAlertRepository fatigueAlertRepository;
    final TripIncidentRepository tripIncidentRepository;
    final DvirInspectionRepository dvirInspectionRepository;
    final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<TripStatus> ACTIVE_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);

    public String execute(Role role, Long roleEntityId, String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case "list_my_orders" -> listMyOrders(roleEntityId);
                case "get_order_status" -> getOrderStatus(input.path("trackingNumber").asText());
                case "list_my_trips" -> listMyTrips(roleEntityId);
                case "get_trip_detail" -> getTripDetail(roleEntityId, input.path("tripId").asLong());
                case "get_fleet_summary" -> getFleetSummary();
                case "list_active_trips" -> listActiveTrips();
                default -> "{\"error\":\"Naməlum alət: " + toolName + "\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"Xəta baş verdi: " + e.getMessage() + "\"}";
        }
    }

    private String listMyOrders(Long customerId) throws Exception {
        List<Map<String, Object>> orders = cargoRepository.findByCustomerId(customerId).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("trackingNumber", c.getTrackingNumber());
                    m.put("description", c.getDescription());
                    m.put("status", c.getStatus());
                    m.put("price", c.getPrice());
                    m.put("paid", c.getPaid());
                    m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());
        return objectMapper.writeValueAsString(orders);
    }

    private String getOrderStatus(String trackingNumber) throws Exception {
        Optional<Cargo> cargoOpt = cargoRepository.findByTrackingNumber(trackingNumber);
        if (cargoOpt.isEmpty()) {
            return "{\"error\":\"Bu tracking nömrəsi ilə sifariş tapılmadı\"}";
        }
        Cargo cargo = cargoOpt.get();
        Trip trip = cargo.getTrip();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trackingNumber", cargo.getTrackingNumber());
        m.put("status", cargo.getStatus());
        m.put("pickupAddress", cargo.getPickupAddress());
        m.put("destinationAddress", cargo.getDestinationAddress());
        m.put("price", cargo.getPrice());
        m.put("paid", cargo.getPaid());
        if (trip != null) {
            m.put("tripStatus", trip.getStatus());
            m.put("driverName", trip.getDriver() != null ? trip.getDriver().getFullName() : null);
            m.put("driverPhone", trip.getDriver() != null ? trip.getDriver().getPhone() : null);
            m.put("vehiclePlate", trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null);
            m.put("deliveredAt", trip.getDeliveredAt() != null ? trip.getDeliveredAt().toString() : null);
        }
        return objectMapper.writeValueAsString(m);
    }

    private String listMyTrips(Long driverId) throws Exception {
        List<Map<String, Object>> trips = tripRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_STATUSES).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tripId", t.getId());
                    m.put("status", t.getStatus());
                    List<Cargo> cargos = cargoRepository.findByTripId(t.getId());
                    if (!cargos.isEmpty()) {
                        m.put("pickupAddress", cargos.get(0).getPickupAddress());
                        m.put("destinationAddress", cargos.get(0).getDestinationAddress());
                    }
                    return m;
                }).collect(Collectors.toList());
        return objectMapper.writeValueAsString(trips);
    }

    private String getTripDetail(Long driverId, Long tripId) throws Exception {
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty() || tripOpt.get().getDriver() == null || !tripOpt.get().getDriver().getId().equals(driverId)) {
            return "{\"error\":\"Bu reys sizə aid deyil və ya tapılmadı\"}";
        }
        Trip trip = tripOpt.get();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tripId", trip.getId());
        m.put("status", trip.getStatus());
        m.put("vehiclePlate", trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null);
        m.put("estimatedDistanceKm", trip.getEstimatedDistanceKm());
        m.put("estimatedCost", trip.getEstimatedCost());
        List<Cargo> cargos = cargoRepository.findByTripId(tripId);
        m.put("cargoCount", cargos.size());
        if (!cargos.isEmpty()) {
            m.put("pickupAddress", cargos.get(0).getPickupAddress());
            m.put("destinationAddress", cargos.get(0).getDestinationAddress());
        }
        return objectMapper.writeValueAsString(m);
    }

    private String getFleetSummary() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendingCargos", cargoRepository.findByStatus(CargoStatus.PENDING).size());
        long activeTrips = tripRepository.findAll().stream()
                .filter(t -> ACTIVE_STATUSES.contains(t.getStatus())).count();
        m.put("activeTrips", activeTrips);
        m.put("unresolvedIncidents", tripIncidentRepository.findByResolvedFalseOrderByCreatedAtDesc().size());
        m.put("unresolvedFatigueAlerts", fatigueAlertRepository.findByResolvedFalseOrderByCreatedAtDesc().size());
        m.put("unresolvedDvirDefects", dvirInspectionRepository.findByHasDefectsTrueAndResolvedFalseOrderByCreatedAtDesc().size());
        return objectMapper.writeValueAsString(m);
    }

    private String listActiveTrips() throws Exception {
        List<Map<String, Object>> trips = tripRepository.findAll().stream()
                .filter(t -> ACTIVE_STATUSES.contains(t.getStatus()))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tripId", t.getId());
                    m.put("status", t.getStatus());
                    m.put("driverName", t.getDriver() != null ? t.getDriver().getFullName() : null);
                    m.put("vehiclePlate", t.getVehicle() != null ? t.getVehicle().getPlateNumber() : null);
                    return m;
                }).collect(Collectors.toList());
        return objectMapper.writeValueAsString(trips);
    }
}
