package com.ltc.logisticsproject.entity;

// Server-tərəfli HOS (Hours of Service) izləməsi — sürücünün hazırkı
// seqmenti ya davam edən sürücülük, ya da fasilə/istirahətdir (bax
// HosSegment, DriverController#hosStatus/hosToggle). Yalnız görünürlük
// üçündür, rəsmi HOS uyğunluq sistemi deyil.
public enum HosStatus {
    DRIVING,
    RESTING
}
