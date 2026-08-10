package com.ltc.logisticsproject.entity;

// Eyni User.verificationCode/verificationCodeExpiresAt cütü həm qeydiyyat
// email-təsdiqi, həm də "şifrəni unutdum" kodu üçün istifadə olunur — bu sahə
// hansı axının kodudur onu bildirir ki, məsələn REGISTER üçün göndərilən kod
// səhvən reset-password addımında istifadə edilə bilməsin (və əksinə).
public enum VerificationPurpose {
    REGISTER,
    PASSWORD_RESET,
    // Profildə email dəyişmə axını (bax ProfileController#requestEmailChange/
    // confirmEmailChange) — kod YENİ email ünvanına göndərilir, təsdiqlənənə
    // qədər User.pendingEmail-də saxlanılır, köhnə email dəyişmir.
    EMAIL_CHANGE
}
