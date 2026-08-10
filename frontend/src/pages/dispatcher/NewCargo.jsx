// Lets a dispatcher key in an order manually (e.g. a phone-in request) using
// the exact same map-based location picker the customer self-service form
// uses (LocationPickerMap: click a street point for a reverse-geocoded
// address, or click a warehouse marker for its saved name/address). Posts to
// POST /api/dispatcher/cargo, which drops the order straight into the same
// PENDING queue CargoQueue.jsx already renders.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import LocationPickerMap from '../../components/LocationPickerMap.jsx';
import { reverseGeocode } from '../../utils/geo.js';

const INCOTERMS = ['EXW', 'FCA', 'FOB', 'CIF', 'CPT', 'DAP', 'DDP'];

export default function NewCargo() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [warehouses, setWarehouses] = useState([]);
  const [form, setForm] = useState({
    customerName: '', customerPhone: '',
    description: '', weight: '', volume: '',
    cargoType: 'GENERAL', urgency: 'STANDARD', requestedPickupDate: '',
    requiresCustoms: false, preferredTransportMode: 'TRUCK', incoterm: 'DDP',
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
    axiosClient.get('/api/dispatcher/warehouses').then((res) => setWarehouses(res.data));
  }, []);

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.customerName || !form.customerPhone) {
      setError(t('dispatcher.newCargoErrCustomer'));
      return;
    }
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
      const res = await axiosClient.post('/api/dispatcher/cargo', {
        customerName: form.customerName,
        customerPhone: form.customerPhone,
        description: form.description,
        weight: form.weight ? parseFloat(form.weight) : null,
        volume: form.volume ? parseFloat(form.volume) : null,
        cargoType: form.cargoType,
        urgency: form.urgency,
        requestedPickupDate: form.requestedPickupDate || null,
        pickupAddress: pickup.name,
        pickupLatitude: pickup.lat,
        pickupLongitude: pickup.lng,
        destinationAddress: form.destinationAddress || destination.name,
        destinationLatitude: destination.lat,
        destinationLongitude: destination.lng,
        requiresCustoms: form.requiresCustoms,
        preferredTransportMode: form.requiresCustoms ? form.preferredTransportMode : null,
        incoterm: form.requiresCustoms ? form.incoterm : null,
        originCountry: form.requiresCustoms ? form.originCountry : null,
        destinationCountry: form.requiresCustoms ? form.destinationCountry : null,
        transitCountries: form.requiresCustoms ? form.transitCountries : null,
      });
      setSuccess(res.data);
    } catch (err) {
      setError(t('dispatcher.newCargoErrCreate'));
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <div className="card success-card" style={{ maxWidth: 480 }}>
        <div className="success-check">✓</div>
        <h2>{t('dispatcher.newCargoSuccessTitle')}</h2>
        <p>{t('dispatcher.newCargoTrackingLabel')}</p>
        <div className="alert alert-success"><strong>{success.trackingNumber}</strong></div>
        <p className="text-muted">{t('dispatcher.newCargoSuccessNote')}</p>
        <button className="btn btn-primary mt-16" onClick={() => navigate('/dispatcher/queue')}>{t('dispatcher.newCargoViewQueueBtn')}</button>
      </div>
    );
  }

  return (
    <div>
      <h2>{t('dispatcher.newCargoTitle')}</h2>
      <p>{t('dispatcher.newCargoDesc')}</p>

      {error && <div className="alert alert-error mt-16">{error}</div>}

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card hover-lift">
          <h3>{t('dispatcher.newCargoMapCardTitle')}</h3>
          <LocationPickerMap
            pickup={pickup}
            destination={destination}
            onPickupChange={handlePickupChange}
            onDestinationChange={handleDestinationChange}
            warehouses={warehouses}
          />
        </div>

        <div className="card hover-lift">
          <h3>{t('dispatcher.newCargoFormCardTitle')}</h3>

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
            <div className="grid grid-2">
              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoCustomerNameLabel')}</label>
                <input className="input" name="customerName" value={form.customerName} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="label">{t('dispatcher.newCargoPhoneLabel')}</label>
                <input className="input" name="customerPhone" value={form.customerPhone} onChange={handleChange} required />
              </div>
            </div>
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

            <div className="form-group" style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 12 }}>
              <label className="flex items-center gap-1.5" style={{ fontWeight: 600, cursor: 'pointer' }}>
                <input type="checkbox" name="requiresCustoms" checked={form.requiresCustoms} onChange={handleChange} />
                {t('dispatcher.newCargoInternationalCheckbox')}
              </label>

              {form.requiresCustoms && (
                <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
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
                    {t('dispatcher.newCargoCustomsHint')}
                  </p>
                </div>
              )}
            </div>

            <button className="btn btn-primary btn-block" type="submit" disabled={submitting || !locationsDone}>
              {submitting ? t('dispatcher.newCargoSubmitting') : t('dispatcher.newCargoSubmitBtn')}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
