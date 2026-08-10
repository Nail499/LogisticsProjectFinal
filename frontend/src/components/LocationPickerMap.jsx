import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapContainer, TileLayer, Marker, useMapEvents, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import MapSearchBox from './MapSearchBox.jsx';

const pickupIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:26px;height:26px;border-radius:50% 50% 50% 0;background:#16a34a;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [26, 26],
  iconAnchor: [13, 26],
});

const destIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:26px;height:26px;border-radius:50% 50% 50% 0;background:#dc2626;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [26, 26],
  iconAnchor: [13, 26],
});

const warehouseIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:20px;height:20px;border-radius:6px;background:#2563eb;border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

function ClickHandler({ mode, onPickupChange, onDestinationChange }) {
  useMapEvents({
    click(e) {
      const { lat, lng } = e.latlng;
      if (mode === 'pickup') onPickupChange(lat, lng);
      else onDestinationChange(lat, lng);
    },
  });
  return null;
}

export default function LocationPickerMap({
  pickup,
  destination,
  onPickupChange,
  onDestinationChange,
  warehouses = [],
  center = [40.4093, 49.8671],
  zoom = 11,
}) {
  const { t } = useTranslation();
  const [mode, setMode] = useState('pickup');

  return (
    <div className="map-picker-wrap">
      <div className="map-mode-toggle">
        <button
          type="button"
          className={`map-mode-btn ${mode === 'pickup' ? 'active-pickup' : ''}`}
          onClick={() => setMode('pickup')}
        >
          🟢 {t('map.pickupModeLabel')} {mode === 'pickup' && t('map.clickMapHint')}
        </button>
        <button
          type="button"
          className={`map-mode-btn ${mode === 'destination' ? 'active-dest' : ''}`}
          onClick={() => setMode('destination')}
        >
          🔴 {t('map.destModeLabel')} {mode === 'destination' && t('map.clickMapHint')}
        </button>
      </div>

      <div className="map-picker">
        {/* Defolt Leaflet zoom düymələri də yuxarı-sol küncdə açılır və
            MapSearchBox-un (ünvan axtarışı) tam-enli zolağının altında
            qalıb onunla üst-üstə düşürdü — zoomControl={false} ilə söndürüb
            ayrıca yuxarı-sağ küncə köçürülür (bax aşağıdakı ZoomControl). */}
        <MapContainer center={center} zoom={zoom} zoomControl={false} style={{ height: '100%', width: '100%' }}>
          <ZoomControl position="topright" />
          <TileLayer
            attribution='&copy; OpenStreetMap'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickHandler mode={mode} onPickupChange={onPickupChange} onDestinationChange={onDestinationChange} />
          <MapSearchBox
            placeholder={mode === 'pickup' ? t('map.pickupSearchPlaceholder') : t('map.destSearchPlaceholder')}
            onSelect={(lat, lng) => (mode === 'pickup' ? onPickupChange(lat, lng) : onDestinationChange(lat, lng))}
          />

          {warehouses.map((w) => (
            w.latitude && w.longitude ? (
              <Marker
                key={w.id}
                position={[w.latitude, w.longitude]}
                icon={warehouseIcon}
                eventHandlers={{
                  click: (e) => {
                    // Marker klikləri default olaraq map-in öz click event-inə "bubble" edir —
                    // bunu dayandırmasaq, altdakı ClickHandler dərhal anbarın dəqiq
                    // koordinatını/adını xam klik koordinatı ilə üstələyir və seçim "tutmur".
                    L.DomEvent.stopPropagation(e);
                    // 4-cü arqument kimi bütün anbar obyektini ötürürük ki, çağıran
                    // tərəf (NewOrder.jsx) bunun bir anbar seçimi olduğunu bilsin və
                    // koordinat əvəzinə anbarın adını/ünvanını göstərsin.
                    if (mode === 'pickup') onPickupChange(w.latitude, w.longitude, w.name, w);
                    else onDestinationChange(w.latitude, w.longitude, w.name, w);
                  },
                }}
              />
            ) : null
          ))}

          {pickup?.lat && <Marker position={[pickup.lat, pickup.lng]} icon={pickupIcon} />}
          {destination?.lat && <Marker position={[destination.lat, destination.lng]} icon={destIcon} />}
        </MapContainer>
      </div>

      <div className="map-coords-preview">
        <span>🟢 {t('map.pickupPreviewLabel')}: <strong>{pickup?.lat ? (pickup.loading ? t('dispatcher.newCargoSearchingAddress') : pickup.name) : t('map.notSelected')}</strong></span>
        <span>🔴 {t('map.destPreviewLabel')}: <strong>{destination?.lat ? (destination.loading ? t('dispatcher.newCargoSearchingAddress') : destination.name) : t('map.notSelected')}</strong></span>
        <span>🔵 {t('map.warehousesHint')}</span>
      </div>
    </div>
  );
}
