import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { User, Phone, FileUp, Truck, Tag, Send, ArrowLeft, CheckCircle2, Search } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

function Field({ label, icon: Icon, children }) {
  return (
    <div>
      <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
        {label}
      </label>
      <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
        <Icon size={17} style={{ color: '#fe8704' }} />
        {children}
      </div>
    </div>
  );
}

function PageShell({ children, wide }) {
  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-12"
      style={{ fontFamily: "'Poppins', sans-serif" }}
    >
      <div className="absolute inset-0 bg-cover bg-center" style={{ backgroundImage: `url(${bannerHero})` }} />
      <div className="absolute inset-0 bg-black/72" />
      <Reveal className={`relative w-full ${wide ? 'max-w-lg' : 'max-w-md'}`}>
        <div className="rounded-2xl bg-white p-8 shadow-2xl sm:p-10">
          <div className="flex items-center justify-center gap-2.5">
            <img src={logoFooter} alt="Fleetra" className="h-9 w-auto" />
            <span className="text-xl font-extrabold" style={{ color: '#111827' }}>
              Fleet<span style={{ color: '#fe8704' }}>ra</span>
            </span>
          </div>
          {children}
        </div>
      </Reveal>
    </div>
  );
}

export default function JobApplicationForm() {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    fullName: '', phone: '', vehiclePlateNumber: '', vehicleBrand: '',
  });
  const [hasOwnVehicle, setHasOwnVehicle] = useState(true);
  const [licenseDocument, setLicenseDocument] = useState(null);
  const [vehicleDocument, setVehicleDocument] = useState(null);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!licenseDocument) {
      setError(t('jobApplication.errNoLicense'));
      return;
    }
    if (hasOwnVehicle && !vehicleDocument) {
      setError(t('jobApplication.errNoVehicleDoc'));
      return;
    }
    setLoading(true);
    try {
      const data = new FormData();
      data.append('fullName', form.fullName);
      data.append('phone', form.phone);
      data.append('hasOwnVehicle', hasOwnVehicle);
      if (hasOwnVehicle) {
        data.append('vehiclePlateNumber', form.vehiclePlateNumber);
        data.append('vehicleBrand', form.vehicleBrand);
      }
      data.append('licenseDocument', licenseDocument);
      if (hasOwnVehicle && vehicleDocument) data.append('vehicleDocument', vehicleDocument);

      // undefined, not 'multipart/form-data' — see ProfilePage.jsx handlePhotoChange
      // for why: an explicit Content-Type without a boundary breaks the upload.
      const res = await axiosClient.post('/api/applications', data, {
        headers: { 'Content-Type': undefined },
      });
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || t('jobApplication.errSubmit'));
    } finally {
      setLoading(false);
    }
  };

  if (result) {
    return (
      <PageShell>
        <div className="mt-7 flex justify-center">
          <span className="flex h-14 w-14 items-center justify-center rounded-full" style={{ background: '#f0fdf4', color: '#16a34a' }}>
            <CheckCircle2 size={28} />
          </span>
        </div>
        <h2 className="mt-4 text-center font-extrabold" style={{ color: '#111827', fontSize: '1.5rem', margin: '16px 0 0' }}>
          {t('jobApplication.receivedTitle')}
        </h2>
        <p className="mt-2 text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>{result.message}</p>
        <div
          className="mt-5 rounded-lg px-4 py-3 text-center text-sm"
          style={{ background: '#fff5ea', color: '#111827', border: '1px solid #fed7aa' }}
        >
          {t('jobApplication.codeLabel')}: <strong>{result.applicationCode}</strong>
        </div>
        <p className="mt-3 text-center text-[13px]" style={{ color: '#6b7280', margin: '12px 0 0' }}>
          {t('jobApplication.saveCodeHint')}
        </p>
        <Link
          to={`/apply/status?code=${result.applicationCode}`}
          className="mt-6 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5"
          style={{ background: '#fe8704', color: '#ffffff', marginTop: '24px' }}
        >
          {t('jobApplication.checkStatusBtn')} <Search size={16} />
        </Link>
        <Link
          to="/"
          className="mt-5 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
          style={{ color: '#111827', marginTop: '20px' }}
        >
          <ArrowLeft size={14} /> {t('applicationStatus.backHome')}
        </Link>
      </PageShell>
    );
  }

  return (
    <PageShell wide>
      <h2 className="text-center font-extrabold" style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}>
        {t('jobApplication.title')}
      </h2>
      <p className="text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
        {t('jobApplication.subtitle')}
      </p>

      {error && (
        <div className="mt-5 rounded-lg px-4 py-3 text-sm" style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
        <div className="text-xs font-bold uppercase tracking-widest" style={{ color: '#fe8704' }}>{t('jobApplication.personalDetailsLabel')}</div>

        <Field label={t('admin.fullNameLabel')} icon={User}>
          <input className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} name="fullName" value={form.fullName} onChange={handleChange} required />
        </Field>
        <Field label={t('admin.colPhone')} icon={Phone}>
          <input className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} name="phone" value={form.phone} onChange={handleChange} required />
        </Field>
        <Field label={t('jobApplication.licenseFileLabel')} icon={FileUp}>
          <input type="file" className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} onChange={(e) => setLicenseDocument(e.target.files[0])} required />
        </Field>

        <div className="mt-2 text-xs font-bold uppercase tracking-widest" style={{ color: '#fe8704' }}>{t('jobApplication.vehicleDetailsLabel')}</div>

        <div className="flex gap-2 rounded-lg border p-1" style={{ borderColor: '#e5e7eb' }}>
          <button
            type="button"
            onClick={() => setHasOwnVehicle(true)}
            className="flex-1 rounded-md py-2 text-xs font-semibold transition-colors"
            style={hasOwnVehicle ? { background: '#fe8704', color: '#ffffff' } : { color: '#6b7280' }}
          >
            {t('jobApplication.hasOwnTruckBtn')}
          </button>
          <button
            type="button"
            onClick={() => setHasOwnVehicle(false)}
            className="flex-1 rounded-md py-2 text-xs font-semibold transition-colors"
            style={!hasOwnVehicle ? { background: '#fe8704', color: '#ffffff' } : { color: '#6b7280' }}
          >
            {t('jobApplication.noOwnTruckBtn')}
          </button>
        </div>

        {hasOwnVehicle && (
          <>
            <Field label={t('jobApplication.plateNumberLabel')} icon={Truck}>
              <input className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} name="vehiclePlateNumber" value={form.vehiclePlateNumber} onChange={handleChange} required={hasOwnVehicle} />
            </Field>
            <Field label={t('admin.brandLabel')} icon={Tag}>
              <input className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} name="vehicleBrand" value={form.vehicleBrand} onChange={handleChange} required={hasOwnVehicle} />
            </Field>
            <Field label={t('jobApplication.vehicleRegFileLabel')} icon={FileUp}>
              <input type="file" className="w-full border-none bg-transparent p-0 text-sm outline-none" style={{ color: '#111827' }} onChange={(e) => setVehicleDocument(e.target.files[0])} required={hasOwnVehicle} />
            </Field>
          </>
        )}

        <button
          type="submit"
          disabled={loading}
          className="mt-2 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5 disabled:opacity-60"
          style={{ background: '#fe8704', color: '#ffffff' }}
        >
          {loading ? t('jobApplication.submitting') : (<>{t('jobApplication.submitBtn')} <Send size={16} /></>)}
        </button>
      </form>

      <p className="mt-6 text-center text-[13px]" style={{ color: '#6b7280', margin: '24px 0 0' }}>
        {t('jobApplication.alreadyAppliedLabel')}{' '}
        <Link to="/apply/status" className="font-semibold" style={{ color: '#fe8704' }}>
          {t('jobApplication.checkYourStatusLink')}
        </Link>
      </p>
      <Link
        to="/"
        className="mt-5 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
        style={{ color: '#111827', marginTop: '20px' }}
      >
        <ArrowLeft size={14} /> {t('applicationStatus.backHome')}
      </Link>
    </PageShell>
  );
}
