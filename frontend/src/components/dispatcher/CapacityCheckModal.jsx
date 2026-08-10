// Stage 4 — enterprise validation: warns the dispatcher before assigning
// more cargo weight than the rated capacity. capacityTons is always the
// TRAILER's rating (see CargoQueue.jsx#effectiveCapacityTons) — the truck
// itself is only the tractor head and never carries cargo, so its own
// capacity is never used here (CargoQueue.jsx also makes trailer selection
// mandatory whenever the selected cargo has weight, before this modal can
// even be reached). Capacity is in tons, Cargo.weight in kg (see entities),
// so the comparison converts capacity -> kg. Purely a frontend guard for
// now (no backend enforcement) — the dispatcher can still confirm and
// override. Restyled to the site's light Fleetra theme.
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { TriangleAlert, X } from 'lucide-react';

export default function CapacityCheckModal({ open, totalWeightKg, capacityTons, onConfirm, onCancel }) {
  const { t } = useTranslation();
  if (!open) return null;

  const capacityKg = (capacityTons || 0) * 1000;
  const overBy = totalWeightKg - capacityKg;
  const pct = capacityKg > 0 ? Math.round((totalWeightKg / capacityKg) * 100) : 0;

  // Portal: DashboardLayout-un ".content"-i "animation: ... both" ilə son
  // kadrda transform saxlayır, bu da position:fixed üçün yeni containing
  // block yaradıb pəncərəni mərkəzdən çıxarır/arxa fonu natamam edir (bax
  // TripDetailModal.jsx-də ətraflı izah).
  return createPortal(
    <div style={{ position: 'fixed', inset: 0, zIndex: 100, background: 'rgba(15,23,42,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <div className="card" style={{ maxWidth: 420, width: '100%', borderColor: 'rgba(220,38,38,0.35)' }}>
        <div className="flex-between">
          <div style={{ display: 'flex', height: 40, width: 40, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(220,38,38,0.12)', color: 'var(--danger)' }}>
            <TriangleAlert size={20} />
          </div>
          <button type="button" onClick={onCancel} className="btn btn-sm" style={{ padding: 6 }}>
            <X size={15} />
          </button>
        </div>

        <h3 className="mt-16" style={{ margin: 0, color: 'var(--danger)' }}>{t('dispatcher.capacityTitle')}</h3>
        <p className="text-muted" style={{ marginTop: 6, fontSize: 13 }}>
          {t('dispatcher.capacityDesc', { pct })}
        </p>

        <div className="mt-16" style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14, display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13 }}>
          <div className="flex-between">
            <span className="text-muted">{t('dispatcher.capacitySelected')}</span>
            <span style={{ fontWeight: 600 }}>{totalWeightKg.toLocaleString()} kg</span>
          </div>
          <div className="flex-between">
            <span className="text-muted">{t('dispatcher.capacityLimit')}</span>
            <span style={{ fontWeight: 600 }}>{capacityKg.toLocaleString()} kg</span>
          </div>
          <div className="flex-between" style={{ borderTop: '1px solid #e5e7eb', paddingTop: 8 }}>
            <span className="text-muted">{t('dispatcher.capacityOverBy')}</span>
            <span style={{ fontWeight: 700, color: 'var(--danger)' }}>+{overBy.toLocaleString()} kg</span>
          </div>
        </div>

        <div className="flex mt-16" style={{ gap: 10 }}>
          <button type="button" onClick={onCancel} className="btn btn-sm" style={{ flex: 1, justifyContent: 'center' }}>
            {t('dispatcher.capacityCancelBtn')}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="btn"
            style={{ flex: 1, justifyContent: 'center', background: 'var(--danger)', borderColor: 'var(--danger)', color: '#fff' }}
          >
            {t('dispatcher.capacityConfirmBtn')}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
