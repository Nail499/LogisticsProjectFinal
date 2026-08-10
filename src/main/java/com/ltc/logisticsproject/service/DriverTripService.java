package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.CargoWaybillDetail;
import com.ltc.logisticsproject.dto.DriverEarningsSummary;
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
    final NotificationService notificationService;

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
        for (Cargo cargo : cargos) {
            if (cargoStatus != null) {
                cargo.setStatus(cargoStatus);
                cargoRepository.save(cargo);
            }
            // Müştəriyə "yükünüz götürüldü/yoldadır/çatdırıldı" bildirişi —
            // CargoStatus enum-unda PICKED_UP ayrıca dəyər olmadığı üçün bu
            // notifikasiya cargoStatus-dan deyil, birbaşa reysin yeni
            // TripStatus-undan asılıdır (bax NotificationService).
            notificationService.notifyCargoStatusChange(cargo, newStatus);
        }

        return trip;
    }

    // Sürücü özünə göndərilmiş reysi qəbul edir — PENDING_ACCEPTANCE-dan
    // PLANNED-a keçid, bax DispatcherService#createTrip (ilkin status).
    @Transactional
    public Trip acceptTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }
        if (trip.getStatus() != TripStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Bu reys artıq qəbul/imtina edilib");
        }

        trip.setStatus(TripStatus.PLANNED);
        return tripRepository.save(trip);
    }

    // Sürücü reysi imtina edir — yüklər avtomatik açılır (trip=null,
    // status=PENDING) ki, dispetçer başqa sürücüyə təhkim edə bilsin, özü isə
    // dispetçer/admin-lərə bildiriş göndərilir (bax
    // NotificationService#notifyTripRejected).
    @Transactional
    public Trip rejectTrip(Long tripId, Long driverId, String reason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Reys tapılmadı"));

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu reys sizə aid deyil");
        }
        if (trip.getStatus() != TripStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Bu reys artıq qəbul/imtina edilib");
        }

        trip.setStatus(TripStatus.REJECTED);
        Trip saved = tripRepository.save(trip);

        List<Cargo> cargos = cargoRepository.findByTripId(tripId);
        for (Cargo cargo : cargos) {
            cargo.setTrip(null);
            cargo.setStatus(CargoStatus.PENDING);
            cargoRepository.save(cargo);
        }

        notificationService.notifyTripRejected(saved, reason);

        return saved;
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

    // "Yük qaiməsi" — bax dto/CargoWaybillDetail-dəki qeyd: bu, kommersiya
    // fakturasından (bax PaymentService#buildInvoice) fərqli sənəddir,
    // yolda DYP/nəzarət yoxlaması üçün lazım olan sənədi təqlid edir (yükün
    // özü, göndərən/alan, ünvanlar — məbləğ YOXDUR). Sürücü yalnız ÖZÜNƏ
    // təhkim olunmuş reysin yükü üçün baxa bilir.
    public CargoWaybillDetail getWaybill(Long cargoId, Long driverId) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RuntimeException("Yük tapılmadı"));
        Trip trip = cargo.getTrip();
        if (trip == null || trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Bu yük sizə aid deyil");
        }

        String senderName = cargo.getOriginWarehouse() != null ? cargo.getOriginWarehouse().getName() : "Fleetra";
        String receiverName = cargo.getCustomer() != null ? cargo.getCustomer().getFullName() : cargo.getCustomerName();
        String receiverPhone = cargo.getCustomer() != null ? cargo.getCustomer().getPhone() : cargo.getCustomerPhone();

        return CargoWaybillDetail.builder()
                .trackingNumber(cargo.getTrackingNumber())
                .description(cargo.getDescription())
                .cargoType(cargo.getCargoType() != null ? cargo.getCargoType().name() : null)
                .weight(cargo.getWeight())
                .volume(cargo.getVolume())
                .senderName(senderName)
                .pickupAddress(cargo.getPickupAddress())
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .destinationAddress(cargo.getDestinationAddress())
                .driverName(trip.getDriver().getFullName())
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .routeInfo(trip.getRouteInfo())
                .createdAt(cargo.getCreatedAt() != null ? cargo.getCreatedAt().toString() : null)
                .build();
    }

    // Sürücü panelində "Qazancım" statistika kartı — bax DriverEarningsSummary
    // qeydi: dəqiq maaş deyil, DELIVERED reyslərin Trip.estimatedCost
    // cəmidir (sistemdə ayrıca komissiya modeli yoxdur). "Bu ay" =
    // deliveredAt cari ay/il ilə üst-üstə düşən reyslər.
    public DriverEarningsSummary getEarningsSummary(Long driverId) {
        List<Trip> delivered = tripRepository.findByDriverIdAndStatus(driverId, TripStatus.DELIVERED);
        LocalDateTime now = LocalDateTime.now();

        int tripsThisMonth = 0;
        double earningsThisMonth = 0;
        int tripsTotal = 0;
        double earningsTotal = 0;

        for (Trip trip : delivered) {
            double cost = trip.getEstimatedCost() != null ? trip.getEstimatedCost() : 0;
            tripsTotal++;
            earningsTotal += cost;
            if (trip.getDeliveredAt() != null
                    && trip.getDeliveredAt().getYear() == now.getYear()
                    && trip.getDeliveredAt().getMonthValue() == now.getMonthValue()) {
                tripsThisMonth++;
                earningsThisMonth += cost;
            }
        }

        return DriverEarningsSummary.builder()
                .tripsThisMonth(tripsThisMonth)
                .earningsThisMonth(Math.round(earningsThisMonth * 100.0) / 100.0)
                .tripsTotal(tripsTotal)
                .earningsTotal(Math.round(earningsTotal * 100.0) / 100.0)
                .build();
    }
}