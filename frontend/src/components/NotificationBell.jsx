import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Bell, CheckCheck } from 'lucide-react';
import axiosClient from '../api/axiosClient';

// Saytın yuxarısındakı (topbar) zəng ikonu — bütün rollarda (bax
// DashboardLayout.jsx) eyni komponent görünür, backend özü hər istifadəçini
// öz bildirişləri ilə filtrləyir (bax NotificationController). Unread say
// 25 saniyədə bir sorğulanır (sadə polling — WebSocket-ə ehtiyac yaratmadan
// zəng ikonunun "canlı" hiss olunması üçün kifayətdir), tam siyahı isə yalnız
// pəncərə açılanda çəkilir.
const POLL_MS = 25000;

function timeAgo(iso, t) {
  const diffMs = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diffMs / 60000);
  if (min < 1) return t('ratings.justNow');
  if (min < 60) return t('ratings.minutesAgo', { count: min });
  const hr = Math.floor(min / 60);
  if (hr < 24) return t('ratings.hoursAgo', { count: hr });
  const day = Math.floor(hr / 24);
  return t('ratings.daysAgo', { count: day });
}

export default function NotificationBell() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);
  const rootRef = useRef(null);

  const fetchUnread = () => {
    axiosClient.get('/api/notifications/unread-count')
      .then((res) => setUnread(res.data.count || 0))
      .catch(() => {});
  };

  const fetchList = () => {
    setLoading(true);
    axiosClient.get('/api/notifications')
      .then((res) => setItems(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchUnread();
    const id = setInterval(fetchUnread, POLL_MS);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const onClickOutside = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const toggleOpen = () => {
    const next = !open;
    setOpen(next);
    if (next) fetchList();
  };

  const handleItemClick = (n) => {
    if (!n.read) {
      axiosClient.post(`/api/notifications/${n.id}/read`).catch(() => {});
      setItems((prev) => prev.map((it) => (it.id === n.id ? { ...it, read: true } : it)));
      setUnread((u) => Math.max(0, u - 1));
    }
    setOpen(false);
    if (n.link) navigate(n.link);
  };

  const handleReadAll = (e) => {
    e.stopPropagation();
    axiosClient.post('/api/notifications/read-all').catch(() => {});
    setItems((prev) => prev.map((it) => ({ ...it, read: true })));
    setUnread(0);
  };

  return (
    <div ref={rootRef} style={{ position: 'relative' }}>
      <button
        type="button"
        onClick={toggleOpen}
        aria-label={t('notif.title')}
        style={{
          position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center',
          width: 38, height: 38, borderRadius: '50%', border: '1px solid #e5e7eb',
          background: '#ffffff', cursor: 'pointer',
        }}
      >
        <Bell size={18} color="#374151" />
        {unread > 0 && (
          <span
            style={{
              position: 'absolute', top: -3, right: -3, minWidth: 17, height: 17, padding: '0 4px',
              borderRadius: 9, background: '#fe8704', color: '#ffffff', fontSize: 10, fontWeight: 700,
              display: 'flex', alignItems: 'center', justifyContent: 'center', border: '2px solid #ffffff',
            }}
          >
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div
          style={{
            position: 'absolute', right: 0, top: 46, width: 360, maxHeight: 440,
            background: '#ffffff', borderRadius: 14, border: '1px solid #e5e7eb',
            boxShadow: '0 12px 32px rgba(15,23,42,0.14)', overflow: 'hidden', zIndex: 200,
            display: 'flex', flexDirection: 'column',
          }}
        >
          <div
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '12px 16px', borderBottom: '1px solid #f1f5f9',
            }}
          >
            <span style={{ fontWeight: 700, fontSize: 14, color: '#111827' }}>{t('notif.title')}</span>
            {unread > 0 && (
              <button
                type="button"
                onClick={handleReadAll}
                style={{
                  display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, fontWeight: 600,
                  color: '#fe8704', background: 'none', border: 'none', cursor: 'pointer', padding: 0,
                }}
              >
                <CheckCheck size={13} /> {t('notif.readAll')}
              </button>
            )}
          </div>

          <div style={{ overflowY: 'auto' }}>
            {loading && (
              <p style={{ padding: 20, textAlign: 'center', color: '#9ca3af', fontSize: 13, margin: 0 }}>{t('common.loading')}</p>
            )}
            {!loading && items.length === 0 && (
              <p style={{ padding: 28, textAlign: 'center', color: '#9ca3af', fontSize: 13, margin: 0 }}>{t('notif.empty')}</p>
            )}
            {!loading && items.map((n) => (
              <div
                key={n.id}
                onClick={() => handleItemClick(n)}
                style={{
                  padding: '12px 16px', display: 'flex', gap: 10, cursor: 'pointer',
                  background: n.read ? '#ffffff' : '#fff7ed',
                  borderBottom: '1px solid #f8fafc',
                }}
              >
                <div
                  style={{
                    width: 8, height: 8, borderRadius: '50%', marginTop: 6, flexShrink: 0,
                    background: n.read ? 'transparent' : '#fe8704',
                  }}
                />
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: n.read ? 500 : 700, color: '#111827' }}>{n.title}</div>
                  <div style={{ fontSize: 12.5, color: '#6b7280', margin: '3px 0 4px', lineHeight: 1.4 }}>{n.message}</div>
                  <div style={{ fontSize: 11, color: '#9ca3af' }}>{timeAgo(n.createdAt, t)}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
