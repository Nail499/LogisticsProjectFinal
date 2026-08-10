package com.ltc.logisticsproject.entity;

// Sürücünün yolda bildirdiyi fövqəladə hal növü — bax entity/TripIncident,
// DriverController#reportIncident.
public enum IncidentType {
    ACCIDENT,
    BREAKDOWN,
    ROAD_CLOSURE,
    OTHER
}
