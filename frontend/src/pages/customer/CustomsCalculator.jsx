// Müştəri gömrük kalkulyatoru — sifariş yaratmadan öncə, mal növü və
// bəyan ediləcək dəyərə görə təxmini gömrük rüsumu/ƏDV/ödəniləcək
// məbləği görmək üçün. Backend CustomsDutyService-in eyni real hesablama
// məntiqini işlədir (admin-in idarə etdiyi tarif cədvəlinə əsasən), ona
// görə burada göstərilən rəqəm real sifariş yaradılanda dispetçerin
// hazırlayacağı bəyannamədəki rəqəmlə üst-üstə düşür.
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Calculator, Info } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

export default function CustomsCalculator() {
  const { t } = useTranslation();
  const [cargoType, setCargoType] = useState('GENERAL');
  const [declaredValue, setDeclaredValue] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const CARGO_TYPES = [
    { value: 'GENERAL', label: t('dispatcher.newCargoTypeGeneral') },
    { value: 'FRAGILE', label: t('dispatcher.newCargoTypeFragile') },
    { value: 'REFRIGERATED', label: t('dispatcher.newCargoTypeRefrigerated') },
    { value: 'HAZARDOUS', label: t('dispatcher.newCargoTypeHazardous') },
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    const value = parseFloat(declaredValue);
    if (!value || value <= 0) {
      setError(t('customer.errDeclaredValue'));
      return;
    }
    setLoading(true);
    try {
      const res = await axiosClient.post('/api/customer/cargo/customs-estimate', {
        cargoType,
        declaredValue: value,
      });
      setResult(res.data);
    } catch {
      setError(t('customer.errCalculate'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>{t('customer.calculatorTitle')}</h2>
      <p>{t('customer.calculatorDesc')}</p>

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card hover-lift">
          <h3>{t('customer.calculatorCardTitle')}</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label">{t('customer.cargoTypeLabel')}</label>
              <select className="input" value={cargoType} onChange={(e) => setCargoType(e.target.value)}>
                {CARGO_TYPES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="label">{t('customer.declaredValueLabel')}</label>
              <input
                className="input"
                value={declaredValue}
                onChange={(e) => setDeclaredValue(e.target.value)}
                placeholder={t('customer.declaredValuePlaceholder')}
                inputMode="decimal"
              />
            </div>
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
              <Calculator size={15} /> {loading ? t('customer.calculating') : t('customer.calculateBtn')}
            </button>
          </form>

          <div className="flex items-start gap-1.5 mt-16" style={{ fontSize: 12 }}>
            <Info size={13} style={{ color: 'var(--text-muted)', flexShrink: 0, marginTop: 2 }} />
            <span className="text-muted">
              {t('customer.calculatorHint')}
            </span>
          </div>
        </div>

        <div className="card" style={{ minHeight: 240 }}>
          <h3>{t('customer.calculatorResultTitle')}</h3>
          {!result && <p className="text-muted mt-16" style={{ fontSize: 13.5 }}>{t('customer.calculatorResultHint')}</p>}
          {result && (
            <div className="mt-16" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <div className="flex-between" style={{ fontSize: 13.5 }}>
                <span className="text-muted">{t('customer.dutyRate', { pct: result.dutyRatePercent })}</span>
                <span style={{ fontWeight: 600 }}>{result.dutyAmount.toFixed(2)} ₼</span>
              </div>
              <div className="flex-between" style={{ fontSize: 13.5 }}>
                <span className="text-muted">{t('customer.vatRate', { pct: result.vatRatePercent })}</span>
                <span style={{ fontWeight: 600 }}>{result.vatAmount.toFixed(2)} ₼</span>
              </div>
              <div className="flex-between" style={{ borderTop: '1px solid #e5e7eb', paddingTop: 10, fontSize: 16 }}>
                <span style={{ fontWeight: 700 }}>{t('customer.totalPayable')}</span>
                <span style={{ fontWeight: 800, color: 'var(--primary)' }}>{result.totalPayable.toFixed(2)} ₼</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
