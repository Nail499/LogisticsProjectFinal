import { useTranslation } from 'react-i18next';
import RatingsList from '../../components/RatingsList.jsx';

// Dispetçer "Reytinqlər" — admin ilə eyni ətraflı görünüş (bax
// DispatcherController#allRatings, AdminRatings.jsx-in analoqu).
export default function DispatcherRatings() {
  const { t } = useTranslation();
  return (
    <div>
      <h2>{t('dispatcher.ratingsTitle')}</h2>
      <p>{t('dispatcher.ratingsDesc')}</p>
      <RatingsList apiUrl="/api/dispatcher/ratings" showDriverColumn />
    </div>
  );
}
