import { useTranslation } from 'react-i18next';

// Reysdəki yüklərin (bax CustomerSummary.price/paid) ödəniş vəziyyətini bir
// balaca nişanda ümumiləşdirir — "Bütün reyslər" cədvəlində "Xərc"
// sütununun yanında göstərilir (bax DispatcherTrips.jsx). Qiyməti hələ
// təyin olunmayan (məs. hələ ödəniş mərhələsinə çatmayan) yüklər sayılmır.
export default function PaymentStatusBadge({ customers }) {
  const { t } = useTranslation();
  const priced = (customers || []).filter((c) => c.price != null);
  if (priced.length === 0) return null;

  const paidCount = priced.filter((c) => c.paid).length;

  if (paidCount === priced.length) {
    return <span className="badge badge-success" style={{ fontSize: 10.5 }}>{t('dispatcher.statusSucceeded')}</span>;
  }
  if (paidCount === 0) {
    return <span className="badge badge-warning" style={{ fontSize: 10.5 }}>{t('dispatcher.unpaidBadge')}</span>;
  }
  return <span className="badge badge-warning" style={{ fontSize: 10.5 }}>{t('dispatcher.partialBadge', { paid: paidCount, total: priced.length })}</span>;
}
