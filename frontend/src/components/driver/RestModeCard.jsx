// Server-tərəfli HOS (iş saatı) izləməsi — köhnə versiya tamamilə client-side
// stopwatch idi (yalnız React state-də saxlanılırdı), sürücü tarayıcını
// bağlayanda/səhifəni yeniləyəndə sayğac sıfırlanırdı. İndi hər DRIVING/
// RESTING keçidi backend-də saxlanılır (bax DriverController#hosStatus/
// hosToggle, HosService, entity/HosSegment) — komponent hər yüklənəndə
// server-dən davam edən seqmenti bərpa edir. UI/UX köhnə versiya ilə eynidir
// (bir toggle açar + saat + irəliləyiş zolağı), sadəcə mənbə serverdir.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BedDouble, AlarmClock } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const FATIGUE_THRESHOLD_HOURS = 4.5;

export default function RestModeCard({ tripId }) {
  const { t } = useTranslation();
  const [status, setStatus] = useState('NONE'); // 'DRIVING' | 'RESTING' | 'NONE'
  const [segmentStartedAt, setSegmentStartedAt] = useState(null);
  const [todayDrivingSeconds, setTodayDrivingSeconds] = useState(0);
  const [serverFatigueWarning, setServerFatigueWarning] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);
  const [nowTick, setNowTick] = useState(Date.now());

  const applyResponse = (data) => {
    setStatus(data.status);
    setSegmentStartedAt(data.segmentStartedAt ? new Date(data.segmentStartedAt) : null);
    setTodayDrivingSeconds(data.todayDrivingSeconds || 0);
    setServerFatigueWarning(!!data.fatigueWarning);
  };

  const loadStatus = () => {
    axiosClient.get(`/api/driver/trips/${tripId}/hos/status`)
      .then((res) => applyResponse(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadStatus(); }, [tripId]);

  // Ekran hər saniyə yenilənir ki, saat canlı işləsin — server-ə hər saniyə
  // sorğu getmir, sadəcə lokal "indi" yenilənir (segmentStartedAt server-dən
  // gələn ankerdir).
  useEffect(() => {
    const tick = setInterval(() => setNowTick(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  // Server-in bugünkü ümumi vaxtı və fatigueWarning bayrağı ilə sinxron
  // qalmaq üçün 30 saniyədə bir status yenidən çəkilir (məs. başqa tab/
  // cihazdan dəyişiklik olubsa, ya da astana yeni keçilibsə).
  useEffect(() => {
    const poll = setInterval(loadStatus, 30000);
    return () => clearInterval(poll);
  }, [tripId]);

  const toggle = async () => {
    setToggling(true);
    try {
      const res = await axiosClient.post(`/api/driver/trips/${tripId}/hos/toggle`);
      applyResponse(res.data);
    } catch {
      // sükutla uğursuz olur — status növbəti pollda özü düzələcək
    } finally {
      setToggling(false);
    }
  };

  if (loading) return null;

  const active = status === 'DRIVING';
  const elapsedSec = active && segmentStartedAt
    ? Math.max(0, Math.floor((nowTick - segmentStartedAt.getTime()) / 1000))
    : 0;
  const hours = Math.floor(elapsedSec / 3600);
  const mins = Math.floor((elapsedSec % 3600) / 60);
  const secs = elapsedSec % 60;
  const pct = Math.min(100, (elapsedSec / 3600 / FATIGUE_THRESHOLD_HOURS) * 100);
  const isWarning = serverFatigueWarning || elapsedSec / 3600 >= FATIGUE_THRESHOLD_HOURS;
  const todayHours = Math.floor(todayDrivingSeconds / 3600);
  const todayMins = Math.floor((todayDrivingSeconds % 3600) / 60);

  return (
    <div
      style={{
        border: `1px solid ${isWarning ? '#fca5a5' : '#e5e7eb'}`,
        background: isWarning ? 'var(--danger-bg)' : '#f9fafb',
        borderRadius: 10,
        padding: 14,
      }}
    >
      <div className="flex-between">
        <div className="flex items-center gap-1.5 text-xs" style={{ fontWeight: 600, color: isWarning ? 'var(--danger)' : 'var(--text-muted)' }}>
          <BedDouble size={13} /> {t('driver.restModeTitle')}
        </div>
        <button
          type="button"
          onClick={toggle}
          disabled={toggling}
          style={{
            position: 'relative',
            height: 24,
            width: 44,
            borderRadius: 999,
            border: 'none',
            cursor: toggling ? 'default' : 'pointer',
            opacity: toggling ? 0.6 : 1,
            background: active ? 'var(--primary)' : '#d1d5db',
            transition: 'background 0.15s',
          }}
        >
          <span
            style={{
              position: 'absolute',
              top: 2,
              left: active ? 22 : 2,
              height: 20,
              width: 20,
              borderRadius: '50%',
              background: '#fff',
              transition: 'left 0.15s',
              boxShadow: '0 1px 2px rgba(0,0,0,0.2)',
            }}
          />
        </button>
      </div>

      {active && (
        <>
          <div
            className="flex items-center gap-1.5"
            style={{ marginTop: 10, fontSize: 22, fontWeight: 800, color: isWarning ? 'var(--danger)' : 'var(--text)' }}
          >
            <AlarmClock size={17} />
            {String(hours).padStart(2, '0')}:{String(mins).padStart(2, '0')}:{String(secs).padStart(2, '0')}
          </div>
          <div style={{ marginTop: 8, height: 6, width: '100%', borderRadius: 999, overflow: 'hidden', background: '#e5e7eb' }}>
            <div
              style={{
                height: '100%',
                borderRadius: 999,
                width: `${pct}%`,
                background: isWarning ? 'var(--danger)' : 'var(--primary)',
                transition: 'width 0.3s',
              }}
            />
          </div>
          <p className="text-muted" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
            {isWarning ? t('driver.restModeWarning') : t('driver.restModeHint', { hours: FATIGUE_THRESHOLD_HOURS })}
          </p>
        </>
      )}

      {/* Bugünkü ümumi sürücülük vaxtı — bax HosService#computeTodayDrivingSeconds. */}
      <p className="text-muted" style={{ marginTop: 8, marginBottom: 0, fontSize: 11.5 }}>
        {t('driver.hosTodayTotal', { hours: todayHours, mins: todayMins })}
      </p>
    </div>
  );
}
