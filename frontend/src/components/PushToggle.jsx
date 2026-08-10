import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BellOff, BellRing } from 'lucide-react';
import { isPushSupported, getCurrentSubscription, subscribeToPush, unsubscribeFromPush } from '../utils/push.js';

// Brauzer push bildirişi keçid düyməsi — DashboardLayout topbar-ında
// ThemeToggle/NotificationBell ilə yanaşı göstərilir (bax utils/push.js,
// backend PushSubscriptionController). Dəstəklənməyən brauzerlərdə (məs.
// Safari-nin köhnə versiyaları) sadəcə görünmür.
export default function PushToggle() {
  const { t } = useTranslation();
  const [supported, setSupported] = useState(false);
  const [subscribed, setSubscribed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isPushSupported()) return;
    setSupported(true);
    getCurrentSubscription().then((sub) => setSubscribed(Boolean(sub)));
  }, []);

  if (!supported) return null;

  const toggle = async () => {
    setLoading(true);
    setError('');
    try {
      if (subscribed) {
        await unsubscribeFromPush();
        setSubscribed(false);
      } else {
        await subscribeToPush();
        setSubscribed(true);
      }
    } catch (err) {
      setError(err.message || t('common.errPushToggle'));
      setTimeout(() => setError(''), 4000);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ position: 'relative' }}>
      <button
        type="button"
        onClick={toggle}
        disabled={loading}
        className="btn btn-sm"
        style={{ padding: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        title={subscribed ? t('common.pushDisable') : t('common.pushEnable')}
        aria-label={t('common.pushAria')}
      >
        {subscribed ? <BellRing size={16} /> : <BellOff size={16} />}
      </button>
      {error && (
        <div
          className="card"
          style={{ position: 'absolute', top: '100%', right: 0, marginTop: 6, padding: '8px 12px', fontSize: 12, width: 220, zIndex: 30, color: 'var(--danger)' }}
        >
          {error}
        </div>
      )}
    </div>
  );
}
