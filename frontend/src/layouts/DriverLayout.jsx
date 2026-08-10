import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import DashboardLayout from '../components/DashboardLayout.jsx';
import SupportChatWidget from '../components/SupportChatWidget.jsx';

export default function DriverLayout() {
  const { t } = useTranslation();

  const links = [
    { to: '/driver', label: t('nav.currentTrip'), end: true },
    { to: '/driver/history', label: t('nav.tripHistory') },
    { to: '/driver/chat', label: t('nav.chat') },
    { to: '/driver/ratings', label: t('nav.myRatings') },
    { to: '/driver/profile', label: t('nav.profile') },
  ];

  return (
    <>
      <DashboardLayout title={t('nav.driverTitle')} links={links} theme="orange">
        <Outlet />
      </DashboardLayout>
      {/* AI dəstək chat-i — bax SupportChatWidget.jsx */}
      <SupportChatWidget />
    </>
  );
}
