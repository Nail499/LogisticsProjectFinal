// Premium multi-step driver application wizard, rendered as a modal overlay
// on top of the landing page. Reuses the existing POST /api/applications
// (multipart) endpoint — no backend changes required.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  X, User, Truck, FileCheck2, ClipboardCheck, UploadCloud, File as FileIcon,
  CheckCircle2, ChevronLeft, ChevronRight, Loader2,
} from 'lucide-react';
import axiosClient from '../api/axiosClient';

const STEP_ICONS = [User, Truck, FileCheck2, ClipboardCheck];
const STEP_LABELS = ['Personal', 'Vehicle', 'Documents', 'Review'];

function FileDropzone({ label, hint, formats, file, onFile }) {
  const [dragOver, setDragOver] = useState(false);

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files?.[0]) onFile(e.dataTransfer.files[0]);
  };

  return (
    <div className="mb-5">
      <label className="mb-2 block text-sm font-semibold" style={{ color: '#374151' }}>{label}</label>
      <label
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-8 text-center transition-colors"
        style={{
          borderColor: dragOver ? '#fe8704' : file ? '#86efac' : '#e5e7eb',
          background: dragOver ? '#fff5ea' : file ? '#f0fdf4' : '#f9fafb',
        }}
      >
        <input
          type="file"
          className="hidden"
          accept=".pdf,.jpg,.jpeg,.png"
          onChange={(e) => e.target.files?.[0] && onFile(e.target.files[0])}
        />
        {file ? (
          <>
            <FileIcon style={{ color: '#16a34a' }} size={26} />
            <span className="max-w-full truncate text-sm font-medium" style={{ color: '#16a34a' }}>{file.name}</span>
            <span className="text-xs" style={{ color: '#9ca3af' }}>{(file.size / 1024).toFixed(0)} KB</span>
          </>
        ) : (
          <>
            <UploadCloud style={{ color: '#fe8704' }} size={26} />
            <span className="text-sm font-medium" style={{ color: '#374151' }}>{hint}</span>
            <span className="text-xs" style={{ color: '#9ca3af' }}>{formats}</span>
          </>
        )}
      </label>
    </div>
  );
}

const inputStyle = {
  color: '#111827',
  borderColor: '#e5e7eb',
  background: '#ffffff',
};

export default function DriverApplyWizard({ open, onClose }) {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({
    fullName: '', phone: '', vehiclePlateNumber: '', vehicleBrand: '',
  });
  const [hasOwnVehicle, setHasOwnVehicle] = useState(true);
  const [licenseDocument, setLicenseDocument] = useState(null);
  const [vehicleDocument, setVehicleDocument] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  if (!open) return null;

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const canProceed = () => {
    if (step === 1) return form.fullName.trim() && form.phone.trim();
    if (step === 2) return !hasOwnVehicle || (form.vehiclePlateNumber.trim() && form.vehicleBrand.trim());
    if (step === 3) return licenseDocument && (!hasOwnVehicle || vehicleDocument);
    return true;
  };

  const goNext = () => { setError(''); if (canProceed()) setStep((s) => Math.min(s + 1, 4)); };
  const goBack = () => { setError(''); setStep((s) => Math.max(s - 1, 1)); };

  const handleSubmit = async () => {
    setError('');
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
      setError(err.response?.data?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      style={{ fontFamily: "'Poppins', sans-serif" }}
    >
      <div className="relative w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 z-10 rounded-full p-1.5 transition-colors hover:bg-gray-100"
          style={{ color: '#6b7280' }}
        >
          <X size={18} />
        </button>

        {result ? (
          <div className="px-8 py-12 text-center">
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full" style={{ background: '#f0fdf4', color: '#16a34a' }}>
              <CheckCircle2 size={34} />
            </div>
            <h3 className="text-xl font-bold" style={{ color: '#111827' }}>Application Received</h3>
            <p className="mt-2 text-sm" style={{ color: '#6b7280' }}>We&rsquo;ll review your documents and get back to you soon.</p>
            <div className="mt-5 rounded-xl px-4 py-3 text-sm" style={{ background: '#fff5ea', border: '1px solid #fed7aa', color: '#111827' }}>
              Application Code: <strong style={{ color: '#fe8704' }}>{result.applicationCode}</strong>
            </div>
            <p className="mt-2 text-xs" style={{ color: '#9ca3af' }}>Save this code to check your application status later.</p>
            <div className="mt-6 flex gap-3">
              <Link
                to={`/apply/status?code=${result.applicationCode}`}
                className="flex-1 rounded-xl py-2.5 text-sm font-semibold transition-transform hover:-translate-y-0.5"
                style={{ background: '#fe8704', color: '#ffffff' }}
              >
                Check Status
              </Link>
              <button
                type="button"
                onClick={onClose}
                className="flex-1 rounded-xl border py-2.5 text-sm font-semibold transition-colors hover:bg-gray-50"
                style={{ borderColor: '#e5e7eb', color: '#374151' }}
              >
                Close
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="border-b px-8 pb-6 pt-8" style={{ borderColor: '#f1f5f9' }}>
              <h3 className="text-xl font-bold" style={{ color: '#111827' }}>Driver Application</h3>
              <p className="mt-1 text-sm" style={{ color: '#6b7280' }}>Apply to work with your own truck</p>

              <div className="mt-6 flex items-center">
                {STEP_LABELS.map((label, i) => {
                  const Icon = STEP_ICONS[i];
                  const idx = i + 1;
                  const done = idx < step;
                  const active = idx === step;
                  return (
                    <div key={label} className="flex flex-1 items-center">
                      <div className="flex flex-col items-center gap-1.5">
                        <div
                          className="flex h-9 w-9 items-center justify-center rounded-full border-2 transition-colors"
                          style={{
                            borderColor: done ? '#16a34a' : active ? '#fe8704' : '#e5e7eb',
                            background: done ? '#f0fdf4' : active ? '#fff5ea' : '#ffffff',
                            color: done ? '#16a34a' : active ? '#fe8704' : '#9ca3af',
                          }}
                        >
                          {done ? <CheckCircle2 size={16} /> : <Icon size={16} />}
                        </div>
                        <span className="text-[10px] font-medium" style={{ color: active || done ? '#374151' : '#9ca3af' }}>
                          {label}
                        </span>
                      </div>
                      {idx < STEP_LABELS.length && (
                        <div className="mx-1 h-0.5 flex-1 rounded" style={{ background: done ? '#16a34a' : '#e5e7eb' }} />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="px-8 py-6">
              {error && (
                <div className="mb-4 rounded-lg px-3 py-2 text-sm" style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626' }}>
                  {error}
                </div>
              )}

              {step === 1 && (
                <div>
                  <div className="mb-4">
                    <label className="mb-1.5 block text-sm font-semibold" style={{ color: '#374151' }}>Full Name</label>
                    <input
                      className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none"
                      style={inputStyle}
                      name="fullName" value={form.fullName} onChange={handleChange}
                    />
                  </div>
                  <div>
                    <label className="mb-1.5 block text-sm font-semibold" style={{ color: '#374151' }}>Phone</label>
                    <input
                      className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none"
                      style={inputStyle}
                      name="phone" value={form.phone} onChange={handleChange} placeholder="+994 XX XXX XX XX"
                    />
                  </div>
                </div>
              )}

              {step === 2 && (
                <div>
                  <div className="mb-4 flex gap-2 rounded-xl border p-1" style={{ borderColor: '#e5e7eb' }}>
                    <button
                      type="button"
                      onClick={() => setHasOwnVehicle(true)}
                      className="flex-1 rounded-lg py-2 text-xs font-semibold transition-colors"
                      style={hasOwnVehicle ? { background: '#fe8704', color: '#ffffff' } : { color: '#6b7280' }}
                    >
                      I have my own truck
                    </button>
                    <button
                      type="button"
                      onClick={() => setHasOwnVehicle(false)}
                      className="flex-1 rounded-lg py-2 text-xs font-semibold transition-colors"
                      style={!hasOwnVehicle ? { background: '#fe8704', color: '#ffffff' } : { color: '#6b7280' }}
                    >
                      Assign me a company truck
                    </button>
                  </div>

                  {hasOwnVehicle && (
                    <>
                      <div className="mb-4">
                        <label className="mb-1.5 block text-sm font-semibold" style={{ color: '#374151' }}>Plate Number</label>
                        <input
                          className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none"
                          style={inputStyle}
                          name="vehiclePlateNumber" value={form.vehiclePlateNumber} onChange={handleChange}
                        />
                      </div>
                      <div className="mb-4">
                        <label className="mb-1.5 block text-sm font-semibold" style={{ color: '#374151' }}>Brand</label>
                        <input
                          className="w-full rounded-xl border px-3.5 py-2.5 text-sm outline-none"
                          style={inputStyle}
                          name="vehicleBrand" value={form.vehicleBrand} onChange={handleChange}
                        />
                      </div>
                    </>
                  )}
                </div>
              )}

              {step === 3 && (
                <div>
                  <FileDropzone
                    label="Driving License"
                    hint="Drag & drop or click to upload"
                    formats="PDF, JPG, PNG"
                    file={licenseDocument}
                    onFile={setLicenseDocument}
                  />
                  {hasOwnVehicle && (
                    <FileDropzone
                      label="Vehicle Registration"
                      hint="Drag & drop or click to upload"
                      formats="PDF, JPG, PNG"
                      file={vehicleDocument}
                      onFile={setVehicleDocument}
                    />
                  )}
                </div>
              )}

              {step === 4 && (
                <div>
                  <h4 className="mb-3 text-sm font-semibold" style={{ color: '#374151' }}>Review your details</h4>
                  <dl className="space-y-2 rounded-xl border p-4 text-sm" style={{ borderColor: '#f1f5f9', background: '#f9fafb' }}>
                    {[
                      ['Full Name', form.fullName],
                      ['Phone', form.phone],
                      ...(hasOwnVehicle ? [
                        ['Plate Number', form.vehiclePlateNumber],
                        ['Brand', form.vehicleBrand],
                        ['Vehicle Registration', vehicleDocument?.name],
                      ] : [['Vehicle', 'Company truck will be assigned']]),
                      ['Driving License', licenseDocument?.name],
                    ].map(([k, v]) => (
                      <div key={k} className="flex justify-between gap-4">
                        <dt style={{ color: '#9ca3af' }}>{k}</dt>
                        <dd className="truncate text-right" style={{ color: '#111827' }}>{v}</dd>
                      </div>
                    ))}
                  </dl>
                </div>
              )}
            </div>

            <div className="flex items-center justify-between border-t px-8 py-5" style={{ borderColor: '#f1f5f9' }}>
              <button
                type="button"
                onClick={goBack}
                disabled={step === 1}
                className="flex items-center gap-1 rounded-xl px-4 py-2 text-sm font-semibold transition-colors disabled:opacity-0"
                style={{ color: '#6b7280' }}
              >
                <ChevronLeft size={16} /> Back
              </button>

              {step < 4 ? (
                <button
                  type="button"
                  onClick={goNext}
                  disabled={!canProceed()}
                  className="flex items-center gap-1 rounded-xl px-5 py-2.5 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-40"
                  style={{ background: '#fe8704', color: '#ffffff' }}
                >
                  Next <ChevronRight size={16} />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleSubmit}
                  disabled={loading}
                  className="flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold transition-colors disabled:opacity-50"
                  style={{ background: '#16a34a', color: '#ffffff' }}
                >
                  {loading && <Loader2 size={16} className="animate-spin" />}
                  {loading ? 'Submitting…' : 'Submit Application'}
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
