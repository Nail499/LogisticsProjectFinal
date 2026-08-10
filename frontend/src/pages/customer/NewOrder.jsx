import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';
import LocationPickerMap from '../../components/LocationPickerMap.jsx';
import Reveal from '../../components/Reveal.jsx';
import { reverseGeocode } from '../../utils/geo.js';

const INCOTERMS = ['EXW', 'FCA', 'FOB', 'CIF', 'CPT', 'DAP', 'DDP'];

// Ölkə daxili sifarişlərdə xəritə Bakıya yaxınlaşdırılıb (anbarlar aydın
// görünsün deyə); xarici ölkə sifarişlərində isə uzaqlaşdırılıb ki, eyni
// baxışda həm Azərbaycandakı anbar (götürülmə), həm də sərhəddən kənar
// təhvil nöqtəsi seçilə bilsin.
const MAP_CONFIG = {
  DOMESTIC: { center: [40.4093, 49.8671], zoom: 11 },
  INTERNATIONAL: { center: [41.5, 40], zoom: 4 },
};

export default function NewOrder() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [warehouses, setWarehouses] = useState([]);
  const [scope, setScope] = useState('DOMESTIC');
  const [form, setForm] = useState({
    description: '', weight: '', volume: '',
    cargoType: 'GENERAL', urgency: 'STANDARD', requestedPickupDate: '',
    preferredTransportMode: 'TRUCK', incoterm: 'DDP',
    originCountry: '', destinationCountry: '', transitCountries: '',
  });
  const [pickup, setPickup] = useState(null);
  const [destination, setDestination] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const CARGO_TYPES = [
    { value: 'GENERAL', label: t('dispatcher.newCargoTypeGeneral'), icon: '📦' },
    { value: 'FRAGILE', label: t('dispatcher.newCargoTypeFragile'), icon: '🔺' },
    { value: 'REFRIGERATED', label: t('dispatcher.newCargoTypeRefrigerated'), icon: '❄️' },
    { value: 'HAZARDOUS', label: t('dispatcher.newCargoTypeHazardous'), icon: '⚠️' },
  ];

  const TRANSPORT_MODES = [
    { value: 'TRUCK', label: t('dispatcher.newCargoTransportTruck') },
    { value: 'RAIL', label: t('dispatcher.newCargoTransportRail') },
    { value: 'SEA', label: t('dispatcher.newCargoTransportSea') },
    { value: 'AIR', label: t('dispatcher.newCargoTransportAir') },
  ];

  useEffect(() => {
    axiosClient.get('/api/customer/cargo/warehouses').then((res) => setWarehouses(res.data));
  }, []);

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  // Ölkə daxili/xarici arasında keçid ayrı xəritə deməkdir — köhnə
  // seçimlər (fərqli miqyasda seçilmiş nöqtələr) yeni rejimdə mənasız
  // olduğu üçün sıfırlanır, istifadəçi təzə xəritədə yenidən seçir.
  const switchScope = (next) => {
    if (next === scope) return;
    setScope(next);
    setPickup(null);
    setDestination(null);
    setForm((f) => ({ ...f, destinationAddress: '' }));
  };

  // `warehouse` is only set when the click was on a warehouse marker (see
  // LocationPickerMap) — in that case we show the warehouse's own saved
  // name/address instead of a street name. Otherwise we reverse-geocode the
  // raw click coordinates into a real street address (küçə adı), not a
  // lat/lng pair.
  const handlePickupChange = (lat, lng, name, warehouse) => {
    if (warehouse) {
      setPickup({ lat, lng, name: warehouse.name, address: warehouse.address, isWarehouse: true, loading: false });
      return;
    }
    setPickup({ lat, lng, name: null, isWarehouse: false, loading: true });
    reverseGeocode(lat, lng).then((address) => {
      setPickup((prev) => (prev && prev.lat === lat && prev.lng === lng ? { ...prev, name: address, loading: false } : prev));
    });
  };

  const handleDestinationChange = (lat, lng, name, warehouse) => {
    if (warehouse) {
      setDestination({ lat, lng, name: warehouse.name, address: warehouse.address, isWarehouse: true, loading: false });
      setForm((f) => ({ ...f, destinationAddress: `${warehouse.name} (${warehouse.address})` }));
      return;
    }
    setDestination({ lat, lng, name: null, isWarehouse: false, loading: true });
    reverseGeocode(lat, lng).then((address) => {
      setDestination((prev) => (prev && prev.lat === lat && prev.lng === lng ? { ...prev, name: address, loading: false } : prev));
      setForm((f) => ({ ...f, destinationAddress: address }));
    });
  };

  const locationsDone = Boolean(pickup && destination);
  const isInternational = scope === 'INTERNATIONAL';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!pickup) {
      setError(t('dispatcher.newCargoErrPickup'));
      return;
    }
    if (!destination) {
      setError(t('dispatcher.newCargoErrDest'));
      return;
    }
    if (pickup.loading || destination.loading) {
      setError(t('dispatcher.newCargoErrWaitAddress'));
      return;
    }
    setSubmitting(true);
    try {
      const res = await axiosClient.post('/api/customer/cargo', {
        ...form,
        weight: form.weight ? parseFloat(form.weight) : null,
        volume: form.volume ? parseFloat(form.volume) : null,
        pickupAddress: pickup.name || (pickup.lat.toFixed(4) + ', ' + pickup.lng.toFixed(4)),
        pickupLatitude: pickup.lat,
        pickupLongitude: pickup.lng,
        destinationAddress: form.destinationAddress || (destination.lat.toFixed(4) + ', ' + destination.lng.toFixed(4)),
        destinationLatitude: destination.lat,
        destinationLongitude: destination.lng,
        requiresCustoms: isInternational,
        preferredTransportMode: isInternational ? form.preferredTransportMode : null,
        incoterm: isInternational ? form.incoterm : null,
        originCountry: isInternational ? form.originCountry : null,
        destinationCountry: isInternational ? form.destinationCountry : null,
        transitCountries: isInternational ? form.transitCountries : null,
      });
      setSuccess(res.data);
    } catch (err) {
      setError(t('customer.errCreateOrder'));
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <Reveal>
        <div className="card success-card" style={{ maxWidth: 480 }}>
          <div className="success-check">✓</div>
          <h2>{t('customer.orderAcceptedTitle')}</h2>
          <p>{t('customer.orderTrackingLabel')}</p>
          <div className="alert alert-success"><strong>{success.trackingNumber}</strong></div>
          <p className="text-muted">{t('customer.orderTrackingNote')}</p>
          <button className="btn btn-primary mt-16" onClick={() => navigate('/customer/orders')}>{t('customer.viewMyOrdersBtn')}</button>
        </div>
      </Reveal>
    );
  }

  return (
    <div>
      <Reveal>
        <h2>{t('customer.newOrderTitle')}</h2>
        <p>{t('customer.newOrderDesc')}</p>
      </Reveal>

      <Reveal delay={20}>
        <div className="flex mt-16" style={{ gap: 10 }}>
          <button
            type="button"
            onClick={() => switchScope('DOMESTIC')}
            className="btn"
            style={{
              flex: 1, justifyContent: 'center', gap: 8, padding: '12px 16px',
              background: scope === 'DOMESTIC' ? 'var(--primary)' : '#fff',
              borderColor: scope === 'DOMESTIC' ? 'var(--primary)' : '#e5e7eb',
              color: scope === 'DOMESTIC' ? '#fff' : '#111827',
              fontWeight: 600,
            }}
          >
            🏠 {t('customer.domesticBtn')}
          </button>
          <button
            type="button"
            onClick={() => switchScope('INTERNATIONAL')}
            className="btn"
            style={{
              flex: 1, justifyContent: 'center', gap: 8, padding: '12px 16px',
              background: scope === 'INTERNATIONAL' ? 'var(--primary)' : '#fff',
              borderColor: scope === 'INTERNATIONAL' ? 'var(--primary)' : '#e5e7eb',
              color: scope === 'INTERNATIONAL' ? '#fff' : '#111827',
              fontWeight: 600,
            }}
          >
            🌍 {t('customer.internationalBtn')}
          </button>
        </div>
      </Reveal>

      <Reveal delay={40}>
        <div className="form-steps mt-16">
          <div className={`form-step-item ${locationsDone ? 'done' : 'active'}`}>
            <div className="form-step-circle">{locationsDone ? '✓' : '1'}</div>
            <span className="form-step-text">{t('customer.stepLocation')}</span>
          </div>
          <div className="form-step-line" />
          <div className={`form-step-item ${locationsDone ? 'active' : ''}`}>
            <div className="form-step-circle">2</div>
            <span className="form-step-text">{t('customer.stepCargoInfo')}</span>
          </div>
        </div>
      </Reveal>

      {error && <div className="alert alert-error mt-16">{error}</div>}

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <Reveal delay={80}>
          <div className="card hover-lift">
            <h3>{isInternational ? t('customer.mapCardTitleInternational') : t('customer.mapCardTitleDomestic')}</h3>
            {isInternational && (
              <p className="text-muted" style={{ marginTop: -6, marginBottom: 10, fontSize: 12.5 }}>
                {t('customer.internationalMapHint')}
              </p>
            )}
            <LocationPickerMap
              key={scope}
              pickup={pickup}
              destination={destination}
              onPickupChange={handlePickupChange}
              onDestinationChange={handleDestinationChange}
              warehouses={warehouses}
              center={MAP_CONFIG[scope].center}
              zoom={MAP_CONFIG[scope].zoom}
            />
          </div>
        </Reveal>

        <Reveal delay={160}>
          <div className="card hover-lift">
            <h3>{t('customer.cargoInfoCardTitle')}</h3>

            <div className="selected-locations-box" style={{ background: '#f7f8fb', border: '1px solid #e5e7eb', borderRadius: 8, padding: 12, marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, marginBottom: 10 }}>
                <span>🟢</span>
                <div>
                  <div style={{ fontSize: 12, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>{t('dispatcher.newCargoPickupLabel')}</div>
                  {!pickup && <div style={{ fontSize: 14, color: '#9ca3af' }}>{t('dispatcher.newCargoNotSelected')}</div>}
                  {pickup?.loading && <div style={{ fontSize: 14, color: '#9ca3af' }}>{t('dispatcher.newCargoSearchingAddress')}</div>}
                  {pickup && !pickup.loading && (
                    <div style={{ fontSize: 14, color: '#111827' }}>
                      {pickup.isWarehouse && <span>🏢 <strong>{pickup.name}</strong></span>}
                      {!pickup.isWarehouse && pickup.name}
                      {pickup.isWarehouse && pickup.address && (
                        <div style={{ fontSize: 12.5, color: '#6b7280' }}>{pickup.address}</div>
                      )}
                    </div>
                  )}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                <span>🔴</span>
                <div>
                  <div style={{ fontSize: 12, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>{t('dispatcher.newCargoDestLabel')}</div>
                  {!destination && <div style={{ fontSize: 14, color: '#9ca3af' }}>{t('dispatcher.newCargoNotSelected')}</div>}
                  {destination?.loading && <div style={{ fontSize: 14, color: '#9ca3af' }}>{t('dispatcher.newCargoSearchingAddress')}</div>}
                  {destination && !destination.loading && (
                    <div style={{ fontSize: 14, color: '#111827' }}>
                      {destination.isWarehouse && <span>🏢 <strong>{destination.name}</strong></span>}
                      {!destination.isWarehouse && destination.name}
                      {destination.isWarehouse && destination.address && (
                        <div style={{ fontSize: 12.5, color: '#6b7280' }}>{destination.address}</div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoDescriptionLabel')}</label>
                <input className="input" name="description" value={form.description} onChange={handleChange} required />
              </div>
              <div className="grid grid-2">
                <div className="form-group">
                  <label className="label">{t('dispatcher.newCargoWeightLabel')}</label>
                  <input className="input" name="weight" value={form.weight} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label className="label">{t('dispatcher.newCargoVolumeLabel')}</label>
                  <input className="input" name="volume" value={form.volume} onChange={handleChange} />
                </div>
              </div>
              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoDestAddressLabel')}</label>
                <input className="input" name="destinationAddress" value={form.destinationAddress || ''} onChange={handleChange} placeholder={t('dispatcher.newCargoDestAddressPlaceholder')} />
              </div>

              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoTypeLabel')}</label>
                <div className="cargo-type-grid">
                  {CARGO_TYPES.map((ct) => (
                    <div
                      key={ct.value}
                      className={`cargo-type-option ${form.cargoType === ct.value ? 'selected' : ''}`}
                      onClick={() => setForm({ ...form, cargoType: ct.value })}
                    >
                      <div className="cargo-type-option-icon">{ct.icon}</div>
                      <div className="cargo-type-option-label">{ct.label}</div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoUrgencyLabel')}</label>
                <select className="input" name="urgency" value={form.urgency} onChange={handleChange}>
                  <option value="STANDARD">{t('dispatcher.newCargoUrgencyStandard')}</option>
                  <option value="EXPRESS">{t('dispatcher.newCargoUrgencyExpress')}</option>
                </select>
              </div>
              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoPickupDateLabel')}</label>
                <input type="date" className="input" name="requestedPickupDate" value={form.requestedPickupDate} onChange={handleChange} />
              </div>

              {isInternational && (
                <div className="form-group" style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 12 }}>
                  <div className="flex items-center gap-1.5" style={{ fontWeight: 600, marginBottom: 12 }}>
                    🌍 {t('customer.internationalInfoTitle')}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    <div className="grid grid-2">
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="label">{t('dispatcher.newCargoTransportModeLabel')}</label>
                        <select className="input" name="preferredTransportMode" value={form.preferredTransportMode} onChange={handleChange}>
                          {TRANSPORT_MODES.map((m) => (
                            <option key={m.value} value={m.value}>{m.label}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="label">Incoterm</label>
                        <select className="input" name="incoterm" value={form.incoterm} onChange={handleChange}>
                          {INCOTERMS.map((i) => (
                            <option key={i} value={i}>{i}</option>
                          ))}
                        </select>
                      </div>
                    </div>
                    <div className="grid grid-2">
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="label">{t('dispatcher.newCargoOriginCountryLabel')}</label>
                        <input className="input" name="originCountry" value={form.originCountry} onChange={handleChange} placeholder={t('dispatcher.newCargoOriginCountryPlaceholder')} />
                      </div>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="label">{t('dispatcher.newCargoDestCountryLabel')}</label>
                        <input className="input" name="destinationCountry" value={form.destinationCountry} onChange={handleChange} placeholder={t('dispatcher.newCargoDestCountryPlaceholder')} />
                      </div>
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="label">{t('dispatcher.newCargoTransitCountriesLabel')}</label>
                      <input className="input" name="transitCountries" value={form.transitCountries} onChange={handleChange} placeholder={t('dispatcher.newCargoTransitCountriesPlaceholder')} />
                    </div>
                    <p className="text-muted" style={{ margin: 0, fontSize: 12 }}>
                      {t('customer.internationalInfoHint')}
                    </p>
                  </div>
                </div>
              )}

              <button className="btn btn-primary btn-block" type="submit" disabled={submitting}>
                {submitting ? t('dispatcher.newCargoSubmitting') : t('customer.submitOrderBtn')}
              </button>
            </form>
          </div>
        </Reveal>
      </div>
    </div>
  );
}
