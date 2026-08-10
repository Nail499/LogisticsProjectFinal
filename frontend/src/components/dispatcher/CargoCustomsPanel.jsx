// Beynəlxalq göndəriş üçün "Gömrük/Sənədlər" paneli — bir Cargo-nun ticarət
// sənədlərini (invoys, mənşə sertifikatı, CMR və s.) yükləmək/yoxlamaq və
// gömrük bəyannaməsini (rüsum+ƏDV hesablanaraq) yaratmaq/göndərmək/gömrükdən
// keçirmək üçün. CustomsDutyService-in real hesablama nəticəsi (dutyAmount/
// vatAmount/totalPayable) birbaşa backend-dən gəlir, burada yalnız göstərilir.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { X, Upload, FileText, CheckCircle2, XCircle, Calculator, Send, ShieldCheck } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const STATUS_BADGE = {
  PENDING: 'badge-neutral',
  VERIFIED: 'badge-success',
  REJECTED: 'badge-danger',
  DRAFT: 'badge-neutral',
  SUBMITTED: 'badge-info',
  CLEARED: 'badge-success',
};

export default function CargoCustomsPanel({ cargo, onClose }) {
  const { t } = useTranslation();
  const [documents, setDocuments] = useState([]);
  const [declaration, setDeclaration] = useState(null);
  const [docType, setDocType] = useState('INVOICE');
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [declForm, setDeclForm] = useState({ originCountry: '', destinationCountry: '', hsCode: '', declaredValue: '', currency: 'AZN' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const DOCUMENT_TYPES = [
    { value: 'INVOICE', label: t('dispatcher.docInvoice') },
    { value: 'PACKING_LIST', label: t('dispatcher.docPackingList') },
    { value: 'CERTIFICATE_OF_ORIGIN', label: t('dispatcher.docCertOrigin') },
    { value: 'CMR', label: t('dispatcher.docCMR') },
    { value: 'BILL_OF_LADING', label: t('dispatcher.docBillOfLading') },
    { value: 'TRANSIT_DOCUMENT', label: t('dispatcher.docTransit') },
    { value: 'OTHER', label: t('dispatcher.docOther') },
  ];

  const load = () => {
    axiosClient.get(`/api/dispatcher/cargo/${cargo.id}/documents`).then((res) => setDocuments(res.data));
    axiosClient.get(`/api/dispatcher/cargo/${cargo.id}/customs-declaration`).then((res) => {
      if (res.status === 204 || !res.data) {
        setDeclaration(null);
      } else {
        setDeclaration(res.data);
        setDeclForm({
          originCountry: res.data.originCountry || '',
          destinationCountry: res.data.destinationCountry || '',
          hsCode: res.data.hsCode || '',
          declaredValue: res.data.declaredValue ?? '',
          currency: res.data.currency || 'AZN',
        });
      }
    }).catch(() => setDeclaration(null));
  };

  useEffect(load, [cargo.id]);

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('type', docType);
      // undefined, not 'multipart/form-data' — see ProfilePage.jsx handlePhotoChange
      // for why: an explicit Content-Type without a boundary breaks the upload.
      await axiosClient.post(`/api/dispatcher/cargo/${cargo.id}/documents`, fd, {
        headers: { 'Content-Type': undefined },
      });
      setFile(null);
      load();
    } catch {
      setError(t('dispatcher.errUploadDoc'));
    } finally {
      setUploading(false);
    }
  };

  const verifyDoc = async (id) => {
    await axiosClient.post(`/api/dispatcher/documents/${id}/verify`);
    load();
  };
  const rejectDoc = async (id) => {
    await axiosClient.post(`/api/dispatcher/documents/${id}/reject`);
    load();
  };
  const deleteDoc = async (id) => {
    if (!window.confirm(t('dispatcher.deleteDocConfirm'))) return;
    await axiosClient.delete(`/api/dispatcher/documents/${id}`);
    load();
  };

  const saveDeclaration = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const res = await axiosClient.post(`/api/dispatcher/cargo/${cargo.id}/customs-declaration`, {
        ...declForm,
        declaredValue: declForm.declaredValue ? parseFloat(declForm.declaredValue) : null,
      });
      setDeclaration(res.data);
    } catch (err) {
      setError(err.response?.data?.message || t('dispatcher.errSaveDecl'));
    } finally {
      setSaving(false);
    }
  };

  const submitDeclaration = async () => {
    try {
      const res = await axiosClient.post(`/api/dispatcher/customs-declaration/${declaration.id}/submit`);
      setDeclaration(res.data);
    } catch (err) {
      setError(err.response?.data?.message || t('dispatcher.errSubmitDecl'));
    }
  };
  const clearDeclaration = async () => {
    try {
      const res = await axiosClient.post(`/api/dispatcher/customs-declaration/${declaration.id}/clear`);
      setDeclaration(res.data);
    } catch (err) {
      setError(err.response?.data?.message || t('dispatcher.errClearDecl'));
    }
  };

  // Portal: bax TripDetailModal.jsx-də ".content"-in transform-animasiyasının
  // position:fixed-i necə pozduğuna dair ətraflı izah.
  return createPortal(
    <div
      style={{ position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}
      onClick={onClose}
    >
      <div className="card" style={{ maxWidth: 620, width: '100%', maxHeight: '86vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div className="flex-between">
          <div>
            <h3 style={{ margin: 0 }}>{t('dispatcher.customsTitle')}</h3>
            <p className="text-muted" style={{ margin: 0, fontSize: 12.5 }}>{cargo.trackingNumber} — {cargo.description}</p>
          </div>
          <button type="button" className="btn btn-sm" onClick={onClose} style={{ padding: 6 }}><X size={15} /></button>
        </div>

        {error && <div className="alert alert-error mt-16">{error}</div>}

        {/* Sənədlər */}
        <div className="mt-16">
          <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px' }}><FileText size={15} /> {t('dispatcher.documentsTitle')}</h4>
          <form onSubmit={handleUpload} className="flex" style={{ gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
            <select className="input" style={{ maxWidth: 220 }} value={docType} onChange={(e) => setDocType(e.target.value)}>
              {DOCUMENT_TYPES.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
            <input className="input" style={{ maxWidth: 220 }} type="file" onChange={(e) => setFile(e.target.files?.[0] || null)} />
            <button className="btn btn-sm" type="submit" disabled={!file || uploading}>
              <Upload size={13} /> {uploading ? t('dispatcher.uploading') : t('dispatcher.uploadBtn')}
            </button>
          </form>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {documents.map((d) => (
              <div key={d.id} className="flex-between" style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '8px 10px', fontSize: 12.5 }}>
                <a href={d.fileUrl} target="_blank" rel="noreferrer" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <FileText size={13} /> {DOCUMENT_TYPES.find((dt) => dt.value === d.type)?.label || d.type}
                </a>
                <div className="flex items-center gap-1.5">
                  <span className={`badge ${STATUS_BADGE[d.status]}`}>{d.status}</span>
                  {d.status === 'PENDING' && (
                    <>
                      <button type="button" className="btn btn-sm" style={{ padding: 4 }} onClick={() => verifyDoc(d.id)} title={t('dispatcher.verifyTitle')}><CheckCircle2 size={13} /></button>
                      <button type="button" className="btn btn-sm" style={{ padding: 4 }} onClick={() => rejectDoc(d.id)} title={t('dispatcher.rejectDocTitle')}><XCircle size={13} /></button>
                    </>
                  )}
                  <button type="button" className="btn btn-sm btn-danger" style={{ padding: 4 }} onClick={() => deleteDoc(d.id)}>{t('common.delete')}</button>
                </div>
              </div>
            ))}
            {documents.length === 0 && <p className="text-muted" style={{ fontSize: 12.5 }}>{t('dispatcher.noDocuments')}</p>}
          </div>
        </div>

        {/* Gömrük bəyannaməsi */}
        <div className="mt-24">
          <h4 className="flex items-center gap-1.5" style={{ margin: '0 0 10px' }}><ShieldCheck size={15} /> {t('dispatcher.declarationTitle')}</h4>

          <form onSubmit={saveDeclaration}>
            <div className="grid grid-2">
              <div className="form-group" style={{ marginBottom: 8 }}>
                <label className="label">{t('dispatcher.newCargoOriginCountryLabel')}</label>
                <input className="input" value={declForm.originCountry} onChange={(e) => setDeclForm({ ...declForm, originCountry: e.target.value })} />
              </div>
              <div className="form-group" style={{ marginBottom: 8 }}>
                <label className="label">{t('dispatcher.newCargoDestCountryLabel')}</label>
                <input className="input" value={declForm.destinationCountry} onChange={(e) => setDeclForm({ ...declForm, destinationCountry: e.target.value })} />
              </div>
            </div>
            <div className="grid grid-2">
              <div className="form-group" style={{ marginBottom: 8 }}>
                <label className="label">{t('dispatcher.hsCodeLabel')}</label>
                <input className="input" value={declForm.hsCode} onChange={(e) => setDeclForm({ ...declForm, hsCode: e.target.value })} placeholder={t('dispatcher.hsCodePlaceholder')} />
              </div>
              <div className="form-group" style={{ marginBottom: 8 }}>
                <label className="label">{t('dispatcher.declaredValueLabel')}</label>
                <input className="input" value={declForm.declaredValue} onChange={(e) => setDeclForm({ ...declForm, declaredValue: e.target.value })} placeholder="₼" />
              </div>
            </div>
            <button className="btn btn-sm" type="submit" disabled={saving || declaration?.status === 'CLEARED'}>
              <Calculator size={13} /> {saving ? t('dispatcher.calculating') : t('dispatcher.calcSaveBtn')}
            </button>
          </form>

          {declaration && (
            <div className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14 }}>
              <div className="flex-between" style={{ flexWrap: 'wrap', gap: 8 }}>
                <span style={{ fontWeight: 700 }}>{declaration.declarationNumber}</span>
                <span className={`badge ${STATUS_BADGE[declaration.status]}`}>{declaration.status}</span>
              </div>
              <div className="grid grid-2 mt-8" style={{ fontSize: 13, gap: 6 }}>
                <div className="flex-between"><span className="text-muted">{t('dispatcher.dutyRate', { pct: declaration.dutyRatePercent })}</span><span>{declaration.dutyAmount?.toFixed(2)} ₼</span></div>
                <div className="flex-between"><span className="text-muted">{t('dispatcher.vatRate', { pct: declaration.vatRatePercent })}</span><span>{declaration.vatAmount?.toFixed(2)} ₼</span></div>
                <div className="flex-between" style={{ gridColumn: '1 / -1', fontWeight: 700, borderTop: '1px solid #e5e7eb', paddingTop: 6 }}>
                  <span>{t('dispatcher.totalPayable')}</span><span style={{ color: 'var(--primary)' }}>{declaration.totalPayable?.toFixed(2)} ₼</span>
                </div>
              </div>
              <div className="flex mt-16" style={{ gap: 8 }}>
                {declaration.status === 'DRAFT' && (
                  <button type="button" className="btn btn-sm btn-primary" onClick={submitDeclaration}><Send size={13} /> {t('dispatcher.submitDeclarationBtn')}</button>
                )}
                {declaration.status === 'SUBMITTED' && (
                  <button type="button" className="btn btn-sm" style={{ background: 'var(--success)', borderColor: 'var(--success)', color: '#fff' }} onClick={clearDeclaration}>
                    <ShieldCheck size={13} /> {t('dispatcher.clearDeclarationBtn')}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
