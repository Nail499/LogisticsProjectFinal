import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import DashboardLayout from '../components/DashboardLayout.jsx';
import SupportChatWidget from '../components/SupportChatWidget.jsx';

export default function CustomerLayout() {
  const { t } = useTranslation();

  const links = [
    { to: '/customer', label: t('nav.customerHome'), end: true },
    { to: '/customer/new', label: t('nav.newOrder') },
    { to: '/customer/orders', label: t('nav.myOrders') },
    { to: '/customer/chat', label: t('nav.chat') },
    { to: '/customer/invoices', label: t('nav.myInvoices') },
    { to: '/customer/customs-calculator', label: t('nav.customsCalculator') },
    { to: '/customer/profile', label: t('nav.profile') },
  ];

  return (
    <>
      <DashboardLayout title={t('nav.customerTitle')} links={links} theme="orange">
        <Outlet />
      </DashboardLayout>
      {/* AI dəstək chat-i — bax SupportChatWidget.jsx */}
      <SupportChatWidget />
    </>
  );
}
