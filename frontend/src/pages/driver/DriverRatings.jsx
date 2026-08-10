import { useTranslation } from 'react-i18next';
import RatingsList from '../../components/RatingsList.jsx';

// Sürücünün öz "Reytinqlərim" səhifəsi — hansı reysdən nə qiymət/şərh
// aldığını görsün (bax DriverController#myRatings). Sürücü adı sütunu
// göstərilmir — özü özünə göstərmək lazımsızdır.
export default function DriverRatings() {
  const { t } = useTranslation();
  return (
    <div>
      <h2>{t('driver.ratingsTitle')}</h2>
      <p>{t('driver.ratingsDesc')}</p>
      <RatingsList apiUrl="/api/driver/ratings" showDriverColumn={false} />
    </div>
  );
}
