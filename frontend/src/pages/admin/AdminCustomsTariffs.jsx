// Beynəlxalq göndərişlər üçün gömrük rüsumu/ƏDV tarif cədvəli. Backend
// (CustomsDutyService) bu cədvəldən Cargo.cargoType-a görə faizi tapır və
// bəyannamə üçün real hesablama aparır — burada admin faizləri idarə edir.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';

const DEFAULT_FORM = { cargoType: 'GENERAL', dutyRatePercent: '5', vatRatePercent: '18', description: '' };

export default function AdminCustomsTariffs() {
  const { t } = useTranslation();
  const [tariffs, setTariffs] = useState([]);
  const [form, setForm] = useState(DEFAULT_FORM);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const CARGO_TYPES = [
    { value: 'GENERAL', label: t('admin.tariffTypeGeneral') },
    { value: 'FRAGILE', label: t('admin.tariffTypeFragile') },
    { value: 'REFRIGERATED', label: t('admin.tariffTypeRefrigerated') },
    { value: 'HAZARDOUS', label: t('admin.tariffTypeHazardous') },
  ];

  const load = () => {
    axiosClient.get('/api/admin/customs-tariffs').then((res) => setTariffs(res.data));
  };

  useEffect(load, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await axiosClient.post('/api/admin/customs-tariffs', {
        ...form,
        dutyRatePercent: parseFloat(form.dutyRatePercent),
        vatRatePercent: parseFloat(form.vatRatePercent),
      });
      setSuccess(t('admin.tariffSaved'));
      setForm(DEFAULT_FORM);
      load();
    } catch {
      setError(t('admin.errSaveTariff'));
    }
  };

  const handleEdit = (tr) => {
    setForm({
      cargoType: tr.cargoType,
      dutyRatePercent: String(tr.dutyRatePercent),
      vatRatePercent: String(tr.vatRatePercent),
      description: tr.description || '',
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm(t('admin.tariffDeleteConfirm'))) return;
    try {
      await axiosClient.delete(`/api/admin/customs-tariffs/${id}`);
      load();
    } catch {
      setError(t('admin.errDeleteTariff'));
    }
  };

  const labelFor = (v) => CARGO_TYPES.find((c) => c.value === v)?.label || v;

  return (
    <div>
      <h2>{t('admin.tariffsTitle')}</h2>
      <p>{t('admin.tariffsDesc')}</p>

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card">
          <h3>{t('admin.tariffAddUpdateTitle')}</h3>
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label">{t('admin.colCargoType')}</label>
              <select className="input" name="cargoType" value={form.cargoType} onChange={handleChange} required>
                {CARGO_TYPES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="label">{t('admin.dutyRateLabel')}</label>
              <input className="input" name="dutyRatePercent" value={form.dutyRatePercent} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.vatRateLabel')}</label>
              <input className="input" name="vatRatePercent" value={form.vatRatePercent} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.noteLabel')}</label>
              <input className="input" name="description" value={form.description} onChange={handleChange} placeholder={t('admin.notePlaceholder')} />
            </div>
            <button className="btn btn-primary btn-block" type="submit">{t('common.save')}</button>
          </form>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>{t('admin.colCargoType')}</th><th>{t('admin.colDuty')}</th><th>{t('admin.colVat')}</th><th>{t('admin.colNote')}</th><th></th></tr>
            </thead>
            <tbody>
              {tariffs.map((tr) => (
                <tr key={tr.id}>
                  <td>{labelFor(tr.cargoType)}</td>
                  <td>{tr.dutyRatePercent}%</td>
                  <td>{tr.vatRatePercent}%</td>
                  <td className="text-muted">{tr.description || '—'}</td>
                  <td className="flex" style={{ gap: 6 }}>
                    <button className="btn btn-sm" onClick={() => handleEdit(tr)}>{t('admin.editBtn')}</button>
                    <button className="btn btn-sm btn-danger" onClick={() => handleDelete(tr.id)}>{t('common.delete')}</button>
                  </td>
                </tr>
              ))}
              {tariffs.length === 0 && (
                <tr><td colSpan={5} className="text-center text-muted">{t('admin.noTariffs')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
