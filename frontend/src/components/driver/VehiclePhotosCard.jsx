import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Camera, Plus, X, Images, Truck } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import PhotoLightbox from '../PhotoLightbox.jsx';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const MAX_DETAIL_PHOTOS = 4;

// Driver-only card (see ProfilePage.jsx) for managing their vehicle's
// photos — 1 main photo (what the customer sees first, on the tracking
// page) plus up to MAX_DETAIL_PHOTOS detail shots for a closer look.
// Clicking any thumbnail opens the same full-page PhotoLightbox the
// customer's tracking view uses, so the driver previews exactly what
// gets shown to them.
export default function VehiclePhotosCard() {
  const { t } = useTranslation();
  const [vehicle, setVehicle] = useState(null);
  const [loading, setLoading] = useState(true);
  // Əvvəllər sorğu uğursuz olanda (backend rebuild olunmayıb, bu sürücüyə
  // maşın təhkim olunmayıb, auth problemi və s.) heç bir .catch() yox idi —
  // kart sadəcə səssizcə "null" qaytarıb yox olurdu, səbəb heç yerdə
  // görünmürdü. İndi əsl xəta mətni kartda göstərilir.
  const [loadError, setLoadError] = useState(null);
  const [uploadingMain, setUploadingMain] = useState(false);
  const [uploadingDetail, setUploadingDetail] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState(null);
  const [notice, setNotice] = useState(null);
  const mainInputRef = useRef(null);
  const detailInputRef = useRef(null);

  const load = () => {
    setLoading(true);
    setLoadError(null);
    axiosClient.get('/api/driver/vehicle')
      .then((res) => setVehicle(res.data))
      .catch((err) => {
        setLoadError(err.response?.data?.message || err.message || t('common.unknownError'));
      })
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const showNotice = (type, text) => {
    setNotice({ type, text });
    setTimeout(() => setNotice(null), 4000);
  };

  const handleMainPhoto = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingMain(true);
    try {
      const fd = new FormData();
      fd.append('photo', file);
      // Content-Type: undefined (not 'multipart/form-data') — see
      // ProfilePage.jsx handlePhotoChange for why the explicit string breaks
      // the upload (missing boundary).
      const res = await axiosClient.post('/api/driver/vehicle/main-photo', fd, { headers: { 'Content-Type': undefined } });
      setVehicle(res.data);
      showNotice('success', t('driver.mainPhotoUploaded'));
    } catch {
      showNotice('error', t('driver.mainPhotoError'));
    } finally {
      setUploadingMain(false);
      e.target.value = '';
    }
  };

  const handleDetailPhoto = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingDetail(true);
    try {
      const fd = new FormData();
      fd.append('photo', file);
      const res = await axiosClient.post('/api/driver/vehicle/detail-photos', fd, { headers: { 'Content-Type': undefined } });
      setVehicle(res.data);
      showNotice('success', t('driver.detailPhotoAdded'));
    } catch (err) {
      showNotice('error', err.response?.data?.message || t('driver.detailPhotoError'));
    } finally {
      setUploadingDetail(false);
      e.target.value = '';
    }
  };

  const handleDeleteDetail = async (url) => {
    if (!window.confirm(t('driver.deletePhotoConfirm'))) return;
    try {
      const res = await axiosClient.delete('/api/driver/vehicle/detail-photos', { params: { url } });
      setVehicle(res.data);
    } catch {
      showNotice('error', t('driver.deletePhotoError'));
    }
  };

  if (loading) {
    return (
      <div className="card">
        <h3>{t('driver.vehicleTitle')}</h3>
        <p className="text-muted mt-8">{t('driver.vehicleLoading')}</p>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="card">
        <h3>{t('driver.vehicleTitle')}</h3>
        <div className="alert alert-error mt-8">
          {t('driver.vehicleLoadError')}: {loadError}
        </div>
        <button type="button" className="btn btn-sm mt-8" onClick={load}>{t('driver.retryBtn')}</button>
      </div>
    );
  }

  if (!vehicle) return null;

  const detailPhotos = vehicle.detailPhotoUrls || [];
  const allPhotoUrls = [vehicle.mainPhotoUrl, ...detailPhotos].filter(Boolean).map((u) => API_BASE + u);
  const openLightboxFor = (url) => {
    const idx = allPhotoUrls.indexOf(API_BASE + url);
    setLightboxIndex(idx >= 0 ? idx : 0);
  };

  return (
    <div className="card hover-lift">
      <h3>{t('driver.vehicleTitle')}</h3>
      <p className="text-muted" style={{ marginTop: -8, marginBottom: 16 }}>
        {vehicle.plateNumber}{vehicle.brand ? ` · ${vehicle.brand}` : ''}
      </p>

      {notice && (
        <div className={`alert ${notice.type === 'error' ? 'alert-error' : 'alert-success'}`} style={{ marginBottom: 12 }}>
          {notice.text}
        </div>
      )}

      {/* Əsas şəkil */}
      <div
        onClick={() => vehicle.mainPhotoUrl && openLightboxFor(vehicle.mainPhotoUrl)}
        style={{
          height: 160, borderRadius: 12, overflow: 'hidden', background: '#f3f4f6', border: '1px solid #e5e7eb',
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: vehicle.mainPhotoUrl ? 'pointer' : 'default',
        }}
      >
        {vehicle.mainPhotoUrl ? (
          <img src={API_BASE + vehicle.mainPhotoUrl} alt={t('driver.vehicleTitle')} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, color: '#9ca3af' }}>
            <Truck size={26} />
            <span style={{ fontSize: 12.5 }}>{t('driver.noPhotoYet')}</span>
          </div>
        )}
      </div>
      <button type="button" className="btn btn-sm mt-8" onClick={() => mainInputRef.current?.click()} disabled={uploadingMain}>
        <Camera size={14} /> {uploadingMain ? t('driver.uploading') : vehicle.mainPhotoUrl ? t('driver.changeMainPhoto') : t('driver.addMainPhoto')}
      </button>
      <input ref={mainInputRef} type="file" accept="image/*" hidden onChange={handleMainPhoto} />

      {/* Ətraflı şəkillər */}
      <div className="mt-16 flex-between">
        <span className="label" style={{ margin: 0 }}>{t('driver.detailPhotosLabel')} ({detailPhotos.length}/{MAX_DETAIL_PHOTOS})</span>
        {allPhotoUrls.length > 0 && (
          <button type="button" className="btn btn-sm" onClick={() => setLightboxIndex(0)}>
            <Images size={13} /> {t('driver.viewDetails')}
          </button>
        )}
      </div>
      <div className="mt-8" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
        {detailPhotos.map((url, i) => (
          <div key={url + i} style={{ position: 'relative', aspectRatio: '1', borderRadius: 8, overflow: 'hidden', border: '1px solid #e5e7eb' }}>
            <img
              src={API_BASE + url}
              alt={`${t('driver.detailAlt')} ${i + 1}`}
              onClick={() => openLightboxFor(url)}
              style={{ width: '100%', height: '100%', objectFit: 'cover', cursor: 'pointer', display: 'block' }}
            />
            <button
              type="button"
              onClick={() => handleDeleteDetail(url)}
              aria-label={t('common.delete')}
              style={{
                position: 'absolute', top: 3, right: 3, width: 20, height: 20, borderRadius: '50%',
                background: 'rgba(0,0,0,0.55)', border: 'none', color: '#fff', display: 'flex',
                alignItems: 'center', justifyContent: 'center', cursor: 'pointer', padding: 0,
              }}
            >
              <X size={12} />
            </button>
          </div>
        ))}
        {detailPhotos.length < MAX_DETAIL_PHOTOS && (
          <button
            type="button"
            onClick={() => detailInputRef.current?.click()}
            disabled={uploadingDetail}
            style={{
              aspectRatio: '1', borderRadius: 8, border: '1.5px dashed #d1d5db', background: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: '#9ca3af',
            }}
          >
            {uploadingDetail ? <span style={{ fontSize: 10 }}>...</span> : <Plus size={18} />}
          </button>
        )}
      </div>
      <input ref={detailInputRef} type="file" accept="image/*" hidden onChange={handleDetailPhoto} />

      {lightboxIndex != null && (
        <PhotoLightbox photos={allPhotoUrls} initialIndex={lightboxIndex} onClose={() => setLightboxIndex(null)} />
      )}
    </div>
  );
}
