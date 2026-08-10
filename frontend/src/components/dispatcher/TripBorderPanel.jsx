// Bir reysin tranzit zamanı keçdiyi sərhəd/gömrük məntəqələrinin jurnalı
// (bax BorderCrossing entity). Trip.status maşınına toxunmadan ayrıca
// izlənir ki, çoxölkəli marşrutda bir neçə keçid qeyd oluna bilsin.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { X, Flag, Plus } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const STATUS_BADGE = { PENDING: 'badge-neutral', CLEARED: 'badge-success', HELD: 'badge-danger' };

export default function TripBorderPanel({ trip, onClose }) {
  const { t, i18n } = useTranslation();
  const [crossings, setCrossings] = useState([]);
  const [form, setForm] = useState({ borderPointName: '', country: '', customsStatus: 'PENDING', notes: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const STATUS_OPTIONS = [
    { value: 'PENDING', label: t('dispatcher.borderStatusPending') },
    { value: 'CLEARED', label: t('dispatcher.borderStatusCleared') },
    { value: 'HELD', label: t('dispatcher.borderStatusHeld') },
  ];

  const load = () => {
    axiosClient.get(`/api/dispatcher/trips/${trip.tripId}/border-crossings`).then((res) => setCrossings(res.data));
  };

  useEffect(load, [trip.tripId]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.borderPointName) return;
    setSaving(true);
    setError('');
    try {
      await axiosClient.post(`/api/dispatcher/trips/${trip.tripId}/border-crossings`, form);
      setForm({ borderPointName: '', country: '', customsStatus: 'PENDING', notes: '' });
      load();
    } catch {
      setError(t('dispatcher.errSaveCrossing'));
    } finally {
      setSaving(false);
    }
  };

  // Portal: bax TripDetailModal.jsx-də ".content"-in transform-animasiyasının
  // position:fixed-i necə pozduğuna dair ətraflı izah.
  return createPortal(
    <div
      style={{ position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}
      onClick={onClose}
    >
      <div className="card" style={{ maxWidth: 480, width: '100%', maxHeight: '82vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div className="flex-between">
          <div>
            <h3 style={{ margin: 0 }}>{t('dispatcher.borderTitle')}</h3>
            <p className="text-muted" style={{ margin: 0, fontSize: 12.5 }}>{trip.vehiclePlate || t('dispatcher.tripFallback', { id: trip.tripId })} — {trip.driverName || '—'}</p>
          </div>
          <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}><X size={15} /></button>
        </div>

        {error && <div className="alert alert-error mt-16">{error}</div>}

        <form onSubmit={handleSubmit} className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 12 }}>
          <div className="grid grid-2">
            <div className="form-group" style={{ marginBottom: 8 }}>
              <label className="label">{t('dispatcher.borderPointLabel')}</label>
              <input className="input" name="borderPointName" value={form.borderPointName} onChange={handleChange} placeholder={t('dispatcher.borderPointPlaceholder')} required />
            </div>
            <div className="form-group" style={{ marginBottom: 8 }}>
              <label className="label">{t('dispatcher.countryLabel')}</label>
              <input className="input" name="country" value={form.country} onChange={handleChange} placeholder={t('dispatcher.countryPlaceholder')} />
            </div>
          </div>
          <div className="form-group" style={{ marginBottom: 8 }}>
            <label className="label">{t('dispatcher.customsStatusLabel')}</label>
            <select className="input" name="customsStatus" value={form.customsStatus} onChange={handleChange}>
              {STATUS_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: 8 }}>
            <label className="label">{t('dispatcher.notesLabel')}</label>
            <input className="input" name="notes" value={form.notes} onChange={handleChange} />
          </div>
          <button className="btn btn-sm btn-primary" type="submit" disabled={saving}>
            <Plus size={13} /> {saving ? t('dispatcher.saving') : t('dispatcher.addCrossingBtn')}
          </button>
        </form>

        <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {crossings.map((c) => (
            <div key={c.id} style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '8px 10px' }}>
              <div className="flex-between">
                <span className="flex items-center gap-1.5" style={{ fontWeight: 600, fontSize: 13 }}>
                  <Flag size={13} style={{ color: 'var(--primary)' }} /> {c.borderPointName}{c.country ? ` (${c.country})` : ''}
                </span>
                <span className={`badge ${STATUS_BADGE[c.customsStatus]}`}>{STATUS_OPTIONS.find((s) => s.value === c.customsStatus)?.label}</span>
              </div>
              {c.crossedAt && <div className="text-muted" style={{ fontSize: 11.5, marginTop: 4 }}>{new Date(c.crossedAt).toLocaleString(i18n.language)}</div>}
              {c.notes && <div style={{ fontSize: 12, marginTop: 4 }}>{c.notes}</div>}
            </div>
          ))}
          {crossings.length === 0 && <p className="text-muted" style={{ fontSize: 12.5 }}>{t('dispatcher.noCrossings')}</p>}
        </div>
      </div>
    </div>,
    document.body
  );
}
