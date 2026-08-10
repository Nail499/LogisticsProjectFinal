package az.fleetra.mobile.config

// Where the Spring Boot backend lives.
//
// - Android emulator: 10.0.2.2 is a special alias for "localhost" on the
//   machine running the emulator — works out of the box if the backend
//   runs on your computer via `mvnw.cmd spring-boot:run`.
// - Physical phone: the phone is a different machine on the same wifi, so
//   10.0.2.2 will NOT work. Replace it with your computer's LAN IP (find it
//   with `ipconfig` on Windows — the "IPv4 Address" under your Wi-Fi
//   adapter), e.g. "http://192.168.1.72:8080/". Make sure the phone and
//   computer are on the same network and that Windows Firewall allows
//   inbound TCP on port 8080.
// - Must end with a trailing slash — Retrofit requires it for the base URL.
object ApiConfig {
    const val BASE_URL = "http://192.168.1.70:8080/"
}
