import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import DashboardLayout from '../components/DashboardLayout.jsx';
import SupportChatWidget from '../components/SupportChatWidget.jsx';

export default function DispatcherLayout() {
  const { t } = useTranslation();

  const links = [
    { to: '/dispatcher', label: t('nav.controlTower'), end: true },
    { to: '/dispatcher/new-cargo', label: t('nav.newOrder') },
    { to: '/dispatcher/queue', label: t('nav.pendingCargo') },
    { to: '/dispatcher/trips', label: t('nav.allTrips') },
    { to: '/dispatcher/trailers', label: t('nav.trailerPool') },
    { to: '/dispatcher/chat', label: t('nav.chat') },
    { to: '/dispatcher/payments', label: t('nav.payments') },
    { to: '/dispatcher/ratings', label: t('nav.ratings') },
    { to: '/dispatcher/profile', label: t('nav.profile') },
  ];

  return (
    <>
      <DashboardLayout title={t('nav.dispatcherTitle')} links={links} theme="orange">
        <Outlet />
      </DashboardLayout>
      {/* AI dəstək chat-i — bax SupportChatWidget.jsx */}
      <SupportChatWidget />
    </>
  );
}
