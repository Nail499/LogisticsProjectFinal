package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.KeptAccountPreview;
import com.ltc.logisticsproject.entity.Driver;
import com.ltc.logisticsproject.entity.DriverStatus;
import com.ltc.logisticsproject.entity.OwnerType;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.Trailer;
import com.ltc.logisticsproject.entity.TransportMode;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.entity.Vehicle;
import com.ltc.logisticsproject.repository.DriverRepository;
import com.ltc.logisticsproject.repository.TrailerRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.repository.VehicleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Bir dəfəlik "test datasını təmizlə" əməliyyatı (bax
// AdminMaintenanceController, frontend AdminMaintenance.jsx) — istifadəçi
// layihəni sıfırdan səliqəli başlatmaq istədi: bütün sifariş/reys/ödəniş/
// sürücü/tır datası silinir, YALNIZ Anbarlar və Gömrük tarifləri (master
// data) toxunulmaz qalır, hər roldan (ADMIN/DISPATCHER/CUSTOMER) ID-si ən
// kiçik olan 1 hesab saxlanılır. Bu, əvvəllər əl ilə DB alətində icra
// edilməli olan eyni SQL ardıcıllığının tətbiq daxilindən (JdbcTemplate ilə)
// işə salınmasıdır — DataSource localhost-dakı Postgres-ə birbaşa qoşulduğu
// üçün bu, sandbox-dan əlçatmaz olan bazaya çatmağın yeganə yoludur.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceService {

    final JdbcTemplate jdbcTemplate;
    final UserRepository userRepository;
    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final TrailerRepository trailerRepository;
    final PasswordEncoder passwordEncoder;

    // Demo sürücülərin hamısı üçün eyni sabit şifrə — bir dəfəlik nümayiş
    // datasıdır, təhlükəsizlik baxımından fərdiləşdirilmiş şifrə lazım
    // deyil (bax AdminApplicationService#approve real təsdiq axınında hər
    // sürücüyə TƏSADÜFİ şifrə verir, bura isə sırf "boş panel görsənməsin"
    // məqsədi daşıyır).
    private static final String DEMO_DRIVER_PASSWORD = "Surucu123!";

    // Silmədən ƏVVƏL hansı hesabın hər roldan saxlanılacağını göstərmək
    // üçün (bax AdminMaintenanceController, frontend AdminMaintenance.jsx) —
    // resetTestData()-dakı "hər roldan ID-si ən kiçik olanı saxla" qaydası
    // ilə eynidir, sadəcə heç nə silmir.
    public List<KeptAccountPreview> previewKeptAccounts() {
        return List.of(Role.ADMIN, Role.DISPATCHER, Role.CUSTOMER).stream()
                .map(role -> userRepository.findFirstByRoleOrderByIdAsc(role)
                        .map(u -> KeptAccountPreview.builder()
                                .role(role.name())
                                .id(u.getId())
                                .username(u.getUsername())
                                .fullName(u.getFullName())
                                .customerId(u.getCustomerId())
                                .build())
                        .orElse(KeptAccountPreview.builder().role(role.name()).build()))
                .toList();
    }

    @Transactional
    public Map<String, Integer> resetTestData() {
        Map<String, Integer> deleted = new LinkedHashMap<>();

        // 1) Reyslərə bağlı alt-cədvəllər (FK: trip_id) — trips-dən əvvəl
        deleted.put("tracking_logs", jdbcTemplate.update("DELETE FROM tracking_logs"));
        deleted.put("trip_expenses", jdbcTemplate.update("DELETE FROM trip_expenses"));
        deleted.put("fatigue_alerts", jdbcTemplate.update("DELETE FROM fatigue_alerts"));
        deleted.put("border_crossings", jdbcTemplate.update("DELETE FROM border_crossings"));

        // 2) Yüklərə bağlı alt-cədvəllər (FK: cargo_id) — cargos-dan əvvəl
        deleted.put("trade_documents", jdbcTemplate.update("DELETE FROM trade_documents"));
        deleted.put("customs_declarations", jdbcTemplate.update("DELETE FROM customs_declarations"));

        // 3) "Plain Long" əlaqəli fəaliyyət/tarixçə cədvəlləri (FK constraint yoxdur)
        deleted.put("payments", jdbcTemplate.update("DELETE FROM payments"));
        deleted.put("chat_messages", jdbcTemplate.update("DELETE FROM chat_messages"));
        deleted.put("ratings", jdbcTemplate.update("DELETE FROM ratings"));
        deleted.put("notifications", jdbcTemplate.update("DELETE FROM notifications"));
        deleted.put("push_subscriptions", jdbcTemplate.update("DELETE FROM push_subscriptions"));
        deleted.put("audit_logs", jdbcTemplate.update("DELETE FROM audit_logs"));
        deleted.put("job_applications", jdbcTemplate.update("DELETE FROM job_applications"));

        // 4) Yüklər (FK: trip_id, customer_id) — trips/customers-dən əvvəl
        deleted.put("cargos", jdbcTemplate.update("DELETE FROM cargos"));

        // 5) Tırların şəkil alt-cədvəli (FK: vehicle_id) — vehicles-dən əvvəl
        deleted.put("vehicle_detail_photos", jdbcTemplate.update("DELETE FROM vehicle_detail_photos"));

        // 6) Reyslər (FK: driver_id, vehicle_id) — drivers/vehicles-dən əvvəl
        deleted.put("trips", jdbcTemplate.update("DELETE FROM trips"));

        // 7) Tırlar
        deleted.put("vehicles", jdbcTemplate.update("DELETE FROM vehicles"));

        // 8) Sürücülər
        deleted.put("drivers", jdbcTemplate.update("DELETE FROM drivers"));

        // 9) İstifadəçilər: hər roldan ən əvvəl yaradılanı saxla, qalanını (o
        // cümlədən bütün DRIVER rolunu) sil
        deleted.put("users", jdbcTemplate.update("""
                DELETE FROM users
                WHERE id NOT IN (
                    SELECT MIN(id) FROM users WHERE role = 'ADMIN'
                    UNION
                    SELECT MIN(id) FROM users WHERE role = 'DISPATCHER'
                    UNION
                    SELECT MIN(id) FROM users WHERE role = 'CUSTOMER'
                )
                """));

        // 10) Müştəri profilləri: yalnız saxlanılan CUSTOMER istifadəçisinə aid olanı saxla
        deleted.put("customers", jdbcTemplate.update("""
                DELETE FROM customers
                WHERE id NOT IN (SELECT customer_id FROM users WHERE customer_id IS NOT NULL)
                """));

        return deleted;
    }

    // Panel boş görsənməsin deyə bir dəfəlik nümunə filo/sürücü datası
    // yaradır (bax frontend AdminMaintenance.jsx "Nümunə data yarat"
    // düyməsi) — YÜK/REYS yaratmır (istifadəçi bunu ayrıca istəyəcək).
    // İdempotent: telefon/plaka nömrəsi artıq mövcuddursa həmin sətir
    // ötürülür (ikinci dəfə klikləmək xəta vermir, sadəcə əlavə etmir).
    @Transactional
    public Map<String, Integer> seedDemoData() {
        Map<String, Integer> created = new LinkedHashMap<>();
        int driversCreated = 0;
        int usersCreated = 0;

        // {fullName, phone, licenseNumber}
        String[][] driverSeed = {
                {"Elvin Məmmədov", "+994501112233", "AZE-EL12345"},
                {"Tural Əliyev", "+994502223344", "AZE-TU23456"},
                {"Namiq Səfərov", "+994503334455", "AZE-NA34567"},
                {"Rəşad Hüseynov", "+994504445566", "AZE-RH45678"},
                {"Kamran Quliyev", "+994505556677", "AZE-KQ56789"},
                {"Vüqar Rzayev", "+994506667788", "AZE-VR67890"},
        };
        Map<String, Driver> driversByName = new LinkedHashMap<>();
        for (String[] d : driverSeed) {
            if (driverRepository.findByPhone(d[1]).isPresent()) continue;
            Driver driver = driverRepository.save(Driver.builder()
                    .fullName(d[0])
                    .phone(d[1])
                    .licenseNumber(d[2])
                    .status(DriverStatus.ACTIVE)
                    .build());
            driversByName.put(d[0], driver);
            driversCreated++;

            if (userRepository.findByUsername(d[1]).isEmpty()) {
                userRepository.save(User.builder()
                        .fullName(d[0])
                        .username(d[1])
                        .password(passwordEncoder.encode(DEMO_DRIVER_PASSWORD))
                        .role(Role.DRIVER)
                        .driverId(driver.getId())
                        .enabled(true)
                        .build());
                usersCreated++;
            }
        }

        int vehiclesCreated = 0;
        // {plateNumber, brand}
        String[][] companyVehicleSeed = {
                {"10-AA-111", "Mercedes Actros"},
                {"90-BB-222", "Volvo FH"},
                {"77-CC-333", "MAN TGX"},
        };
        for (String[] v : companyVehicleSeed) {
            if (vehicleRepository.findAll().stream().anyMatch(existing -> existing.getPlateNumber().equals(v[0]))) continue;
            vehicleRepository.save(Vehicle.builder()
                    .plateNumber(v[0])
                    .brand(v[1])
                    .transportMode(TransportMode.TRUCK)
                    .ownerType(OwnerType.COMPANY)
                    .build());
            vehiclesCreated++;
        }

        int trailersCreated = 0;
        // {plateNumber, capacity}
        String[][] companyTrailerSeed = {
                {"10-PR-001", "24"},
                {"90-PR-002", "20"},
        };
        for (String[] t : companyTrailerSeed) {
            if (trailerRepository.findAll().stream().anyMatch(existing -> existing.getPlateNumber().equals(t[0]))) continue;
            trailerRepository.save(Trailer.builder()
                    .plateNumber(t[0])
                    .capacity(Double.parseDouble(t[1]))
                    .ownerType(OwnerType.COMPANY)
                    .build());
            trailersCreated++;
        }

        // Sürücüyə məxsus tırlar/qoşqular — yalnız həmin sürücü bu dəfə
        // yaradılıbsa bağlana bilər (əvvəllər mövcud idisə driversByName-da yoxdur).
        // {driverFullName, vehiclePlate, vehicleBrand, trailerPlate(optional), trailerCapacity(optional)}
        String[][] ownedSeed = {
                {"Elvin Məmmədov", "50-EL-777", "Scania R450", "50-EL-778", "24"},
                {"Tural Əliyev", "55-TU-555", "DAF XF", null, null},
                {"Namiq Səfərov", "99-NA-999", "Iveco Stralis", "99-NA-998", "23"},
        };
        for (String[] o : ownedSeed) {
            Driver driver = driversByName.get(o[0]);
            if (driver == null) continue; // sürücü artıq mövcud idi — təkrar bağlama etmirik
            if (vehicleRepository.findAll().stream().noneMatch(existing -> existing.getPlateNumber().equals(o[1]))) {
                vehicleRepository.save(Vehicle.builder()
                        .plateNumber(o[1])
                        .brand(o[2])
                        .transportMode(TransportMode.TRUCK)
                        .ownerType(OwnerType.DRIVER_OWNED)
                        .driver(driver)
                        .build());
                vehiclesCreated++;
            }
            if (o[3] != null && trailerRepository.findAll().stream().noneMatch(existing -> existing.getPlateNumber().equals(o[3]))) {
                trailerRepository.save(Trailer.builder()
                        .plateNumber(o[3])
                        .capacity(Double.parseDouble(o[4]))
                        .ownerType(OwnerType.DRIVER_OWNED)
                        .driver(driver)
                        .build());
                trailersCreated++;
            }
        }

        created.put("drivers", driversCreated);
        created.put("driver_users", usersCreated);
        created.put("vehicles", vehiclesCreated);
        created.put("trailers", trailersCreated);
        return created;
    }

    // İstifadəçi resetTestData()-dan sonra saxlanılan 1 DISPATCHER + 1
    // CUSTOMER hesabını da silib özü sıfırdan yaratmaq istədi — ADMIN,
    // sürücülər və tırlar/qoşqular (bax seedDemoData) TOXUNULMAZ qalır.
    // Cargo.customer real @JoinColumn FK-dır (bax Cargo entity), ona görə
    // customers-i silmədən əvvəl həmin müştərilərə bağlı yükləri (əgər
    // varsa) əvvəlcə silmək lazımdır — normalda bu sıfırdır, amma müdafiə
    // xarakterli.
    @Transactional
    public Map<String, Integer> wipeDispatcherAndCustomerAccounts() {
        Map<String, Integer> deleted = new LinkedHashMap<>();

        deleted.put("cargos", jdbcTemplate.update("""
                DELETE FROM cargos
                WHERE customer_id IN (
                    SELECT customer_id FROM users WHERE role = 'CUSTOMER' AND customer_id IS NOT NULL
                )
                """));

        deleted.put("customers", jdbcTemplate.update("""
                DELETE FROM customers
                WHERE id IN (
                    SELECT customer_id FROM users WHERE role = 'CUSTOMER' AND customer_id IS NOT NULL
                )
                """));

        deleted.put("users", jdbcTemplate.update("""
                DELETE FROM users WHERE role IN ('DISPATCHER', 'CUSTOMER')
                """));

        return deleted;
    }
}
