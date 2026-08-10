import { useTranslation } from 'react-i18next';
import { Headset, User } from 'lucide-react';
import ChatHub from '../../components/ChatHub.jsx';

export default function DriverChat() {
  const { t } = useTranslation();
  return (
    <ChatHub
      title={t('driver.chatTitle')}
      subtitle={t('driver.chatSubtitle')}
      emptyText={t('driver.chatEmpty')}
      tabs={[
        { key: 'CUSTOMER_DRIVER', label: t('driver.chatTabCustomer'), icon: <User size={13} /> },
        { key: 'INTERNAL', label: t('driver.chatTabDispatcher'), icon: <Headset size={13} /> },
      ]}
    />
  );
}
