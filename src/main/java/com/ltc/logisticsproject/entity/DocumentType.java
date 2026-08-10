package com.ltc.logisticsproject.entity;

// Beynəlxalq göndərişlərdə tələb olunan ticarət sənədlərinin növləri (bax
// TradeDocument). CMR — avtomobil daşımalarında beynəlxalq yük qaiməsi;
// BILL_OF_LADING — dəniz daşımalarında konosament; TRANSIT_DOCUMENT — üçüncü
// ölkələrdən keçən yüklər üçün (T1 və analoqu).
public enum DocumentType {
    INVOICE,
    PACKING_LIST,
    CERTIFICATE_OF_ORIGIN,
    CMR,
    BILL_OF_LADING,
    TRANSIT_DOCUMENT,
    OTHER
}
