import { useTranslation } from 'react-i18next';
import RatingsList from '../../components/RatingsList.jsx';

// Admin "Reytinqlər" — bütün sürücülər üzrə hansı reysdən nə qiymət/şərh
// gəlib, ətraflı görmək üçün (bax RatingService#getAllRatingsDetailed).
export default function AdminRatings() {
  const { t } = useTranslation();
  return (
    <div>
      <h2>{t('dispatcher.ratingsTitle')}</h2>
      <p>{t('dispatcher.ratingsDesc')}</p>
      <RatingsList apiUrl="/api/admin/ratings" showDriverColumn />
    </div>
  );
}
