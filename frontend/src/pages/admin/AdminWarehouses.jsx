import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapContainer, TileLayer, Marker, useMapEvents, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import axiosClient from '../../api/axiosClient';
import { reverseGeocode } from '../../utils/geo.js';
import MapSearchBox from '../../components/MapSearchBox.jsx';

const warehouseIcon = new L.DivIcon({
  className: 'custom-pin',
  html: '<div style="width:22px;height:22px;border-radius:6px;background:#2563eb;border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);"></div>',
  iconSize: [22, 22],
  iconAnchor: [11, 11],
});

function ClickHandler({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

export default function AdminWarehouses() {
  const { t } = useTranslation();
  const [warehouses, setWarehouses] = useState([]);
  const [form, setForm] = useState({ name: '', address: '', latitude: '', longitude: '' });
  const [error, setError] = useState('');
  const [resolvedAddress, setResolvedAddress] = useState(null);
  const [geocoding, setGeocoding] = useState(false);

  const load = () => {
    axiosClient.get('/api/admin/warehouses').then((res) => setWarehouses(res.data));
  };

  useEffect(load, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleMapPick = (lat, lng) => {
    setForm((f) => ({ ...f, latitude: lat.toFixed(6), longitude: lng.toFixed(6) }));
    setGeocoding(true);
    setResolvedAddress(null);
    reverseGeocode(lat, lng).then((address) => {
      setResolvedAddress(address);
      setGeocoding(false);
      // Ünvan sahəsi boşdursa, tapılan küçə adını avtomatik dolduraq —
      // admin istəsə əl ilə düzəldə bilər.
      setForm((f) => (f.address ? f : { ...f, address }));
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.latitude || !form.longitude) {
      setError(t('admin.errSelectLocation'));
      return;
    }
    try {
      await axiosClient.post('/api/admin/warehouses', {
        ...form,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
      });
      setForm({ name: '', address: '', latitude: '', longitude: '' });
      setResolvedAddress(null);
      load();
    } catch {
      setError(t('admin.errCreateWarehouse'));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm(t('admin.warehouseDeleteConfirm'))) return;
    setError('');
    try {
      await axiosClient.delete(`/api/admin/warehouses/${id}`);
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('admin.errDeleteWarehouse'));
    }
  };

  const markerPosition = form.latitude && form.longitude ? [parseFloat(form.latitude), parseFloat(form.longitude)] : null;

  return (
    <div>
      <h2>{t('admin.warehousesTitle')}</h2>
      <p>{t('admin.warehousesDesc')}</p>

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card">
          <h3>{t('admin.newWarehouseTitle')}</h3>
          {error && <div className="alert alert-error">{error}</div>}

          <div style={{ height: 280, borderRadius: 8, overflow: 'hidden', marginBottom: 16 }}>
            {/* Defolt zoom düymələri yuxarı-sol küncdə MapSearchBox-un tam-enli
                axtarış zolağının altında qalıb üst-üstə düşürdü — söndürüb
                yuxarı-sağ küncə köçürülür. */}
            <MapContainer center={[40.4093, 49.8671]} zoom={11} zoomControl={false} style={{ height: '100%', width: '100%' }}>
              <ZoomControl position="topright" />
              <TileLayer attribution='&copy; OpenStreetMap' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              <ClickHandler onPick={handleMapPick} />
              <MapSearchBox placeholder={t('admin.warehouseSearchPlaceholder')} onSelect={(lat, lng) => handleMapPick(lat, lng)} />
              {markerPosition && <Marker position={markerPosition} icon={warehouseIcon} />}
            </MapContainer>
          </div>
          <p className="text-muted" style={{ fontSize: 13, marginTop: -10, marginBottom: 16 }}>
            📍 {!markerPosition && t('admin.clickMapHint')}
            {markerPosition && geocoding && t('dispatcher.newCargoSearchingAddress')}
            {markerPosition && !geocoding && (resolvedAddress || `${form.latitude}, ${form.longitude}`)}
          </p>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label">{t('admin.nameLabel')}</label>
              <input className="input" name="name" value={form.name} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.addressLabel')}</label>
              <input className="input" name="address" value={form.address} onChange={handleChange} required />
            </div>
            <div className="grid grid-2">
              <div className="form-group">
                <label className="label">{t('admin.latLabel')}</label>
                <input className="input" name="latitude" value={form.latitude} onChange={handleChange} placeholder={t('admin.mapPlaceholder')} />
              </div>
              <div className="form-group">
                <label className="label">{t('admin.lngLabel')}</label>
                <input className="input" name="longitude" value={form.longitude} onChange={handleChange} placeholder={t('admin.mapPlaceholder')} />
              </div>
            </div>
            <button className="btn btn-primary btn-block" type="submit">{t('admin.addBtn')}</button>
          </form>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>{t('admin.colName')}</th><th>{t('admin.colAddress')}</th><th>{t('admin.colCoord')}</th><th></th></tr>
            </thead>
            <tbody>
              {warehouses.map((w) => (
                <tr key={w.id}>
                  <td>{w.name}</td>
                  <td>{w.address}</td>
                  <td>
                    {w.latitude && w.longitude
                      ? <span>{w.latitude.toFixed(4)}, {w.longitude.toFixed(4)}</span>
                      : <span className="alert-error" style={{ padding: '2px 8px', borderRadius: 4, fontSize: 12 }}>{t('admin.noCoordinates')}</span>}
                  </td>
                  <td><button className="btn btn-sm btn-danger" onClick={() => handleDelete(w.id)}>{t('common.delete')}</button></td>
                </tr>
              ))}
              {warehouses.length === 0 && (
                <tr><td colSpan={4} className="text-center text-muted">{t('admin.noWarehousesTable')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
