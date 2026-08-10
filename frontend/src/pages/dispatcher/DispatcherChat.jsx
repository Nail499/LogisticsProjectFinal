import { useTranslation } from 'react-i18next';
import { Headset, Truck } from 'lucide-react';
import ChatHub from '../../components/ChatHub.jsx';

export default function DispatcherChat() {
  const { t } = useTranslation();
  return (
    <ChatHub
      title={t('dispatcher.chatTitle')}
      subtitle={t('dispatcher.chatSubtitle')}
      emptyText={t('dispatcher.chatEmpty')}
      tabs={[
        { key: 'CUSTOMER_DISPATCHER', label: t('dispatcher.chatTabCustomer'), icon: <Headset size={13} /> },
        { key: 'INTERNAL', label: t('dispatcher.chatTabDriver'), icon: <Truck size={13} /> },
      ]}
    />
  );
}
