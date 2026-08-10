// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Yalnız app/build.gradle.kts-də, VƏ yalnız google-services.json faylı
    // mövcuddursa tətbiq olunur (bax app/build.gradle.kts-dəki şərti apply
    // bloku) — istifadəçi öz Firebase layihəsini qoşmayana qədər build
    // sınmasın deyə.
    alias(libs.plugins.google.services) apply false
}
