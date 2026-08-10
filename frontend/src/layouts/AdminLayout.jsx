import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import DashboardLayout from '../components/DashboardLayout.jsx';

export default function AdminLayout() {
  const { t } = useTranslation();

  const links = [
    { to: '/admin', label: t('nav.overview'), end: true },
    { to: '/admin/applications', label: t('nav.applications') },
    { to: '/admin/drivers', label: t('nav.drivers') },
    { to: '/admin/warehouses', label: t('nav.warehouses') },
    { to: '/admin/vehicles', label: t('nav.vehicles') },
    { to: '/admin/trips', label: t('nav.allTrips') },
    { to: '/admin/dispatchers', label: t('nav.dispatcherAccounts') },
    { to: '/admin/customs-tariffs', label: t('nav.customsTariffs') },
    { to: '/admin/anomalies', label: t('nav.anomalies') },
    { to: '/admin/payments', label: t('nav.payments') },
    { to: '/admin/chat', label: t('nav.chat') },
    { to: '/admin/ratings', label: t('nav.ratings') },
    { to: '/admin/audit-log', label: t('nav.auditLog') },
    { to: '/admin/profile', label: t('nav.profile') },
  ];

  return (
    <DashboardLayout title={t('nav.adminTitle')} links={links} theme="orange">
      <Outlet />
    </DashboardLayout>
  );
}
