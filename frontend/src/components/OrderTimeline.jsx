import { useTranslation } from 'react-i18next';
import { XCircle } from 'lucide-react';

const STEPS = ['PENDING', 'ASSIGNED', 'IN_TRANSIT', 'DELIVERED'];
const STEP_KEY = { PENDING: 'stepPending', ASSIGNED: 'stepAssigned', IN_TRANSIT: 'stepInTransit', DELIVERED: 'stepDelivered' };

// Badge className lookup — the display TEXT for each status now comes from
// the "status.*" i18n namespace (keys match the enum values 1:1: PENDING,
// ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED), so callers should use
// `t('status.' + order.status)` for the label and STATUS_CLASS[order.status]
// for the badge color, instead of the old static STATUS_LABELS[...].label.
export const STATUS_CLASS = {
  PENDING: 'badge-warning',
  ASSIGNED: 'badge-info',
  IN_TRANSIT: 'badge-info',
  DELIVERED: 'badge-success',
  // Dispetçer "Gözləyən yüklər"dən imtina edəndə (bax
  // DispatcherController#rejectCargo) — normal 4 addımlı xəttdə deyil,
  // ayrıca "ləğv edildi" görünüşü ilə göstərilir (bax aşağıda).
  CANCELLED: 'badge-danger',
};

export default function OrderTimeline({ status, cancelReason }) {
  const { t } = useTranslation();

  if (status === 'CANCELLED') {
    return (
      <div className="order-timeline flex items-center gap-1.5" style={{ padding: '6px 0', color: 'var(--danger)', fontSize: 13 }}>
        <XCircle size={15} /> {t('status.orderCancelled')}{cancelReason ? `: ${cancelReason}` : ''}
      </div>
    );
  }

  const currentIndex = STEPS.indexOf(status);
  return (
    <div className="order-timeline">
      {STEPS.map((s, i) => (
        <div key={s} className={`timeline-step ${i < currentIndex ? 'done' : ''} ${i === currentIndex ? 'active' : ''}`}>
          <div className="timeline-line" />
          <div className="timeline-dot">{i < currentIndex ? '✓' : i + 1}</div>
          <div className="timeline-label">{t(`status.${STEP_KEY[s]}`)}</div>
        </div>
      ))}
    </div>
  );
}
