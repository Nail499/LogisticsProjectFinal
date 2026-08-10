// Stage 4 — "Recommended Return Cargo" / backhaul-empty-miles matcher.
// Once a trip is DELIVERED, the truck is sitting empty at the destination.
// This widget scans currently-pending cargo and suggests nearby pickups
// (haversine distance, client-side) so the dispatcher can route the truck
// back with a paying load instead of driving home empty.
// Restyled to the site's light Fleetra theme; each suggested cargo's
// customer name is now clickable to show full contact details.
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PackageSearch, ArrowRightLeft, MapPin, User } from 'lucide-react';
import { closestByDistance } from '../../utils/geo';
import CustomerInfoModal from './CustomerInfoModal.jsx';

const MATCH_RADIUS_KM = 150;

function cargoToCustomer(c) {
  return {
    fullName: c.customer?.fullName || c.customerName,
    phone: c.customer?.phone || c.customerPhone,
    email: c.customer?.email,
    companyName: c.customer?.companyName,
    registered: Boolean(c.customer),
    trackingNumber: c.trackingNumber,
    pickupAddress: c.pickupAddress,
    // Bax CargoQueue.jsx-dəki eyni qeyd — cargoId olmadan "Yazış" düyməsi
    // CustomerInfoModal-da görünmür.
    cargoId: c.id,
  };
}

export default function BackhaulMatcher({ liveTrips, pendingCargo }) {
  const { t } = useTranslation();
  const [customerModalCargo, setCustomerModalCargo] = useState(null);
  const customerModalCustomers = useMemo(
    () => (customerModalCargo ? [cargoToCustomer(customerModalCargo)] : null),
    [customerModalCargo]
  );

  const matches = useMemo(() => {
    const delivered = liveTrips.filter(
      (tr) => tr.status === 'DELIVERED' && tr.destinationLatitude != null && tr.destinationLongitude != null
    );

    return delivered
      .map((trip) => {
        const nearby = closestByDistance(
          trip.destinationLatitude,
          trip.destinationLongitude,
          pendingCargo,
          3,
          (c) => [c.pickupLatitude, c.pickupLongitude]
        ).filter((c) => c.distanceKm <= MATCH_RADIUS_KM);
        return { trip, nearby };
      })
      .filter((m) => m.nearby.length > 0);
  }, [liveTrips, pendingCargo]);

  if (matches.length === 0) return null;

  return (
    <div className="card mt-24" style={{ borderColor: 'rgba(22,163,74,0.3)' }}>
      <div className="flex items-center gap-2">
        <div style={{ display: 'flex', height: 32, width: 32, alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: 'rgba(22,163,74,0.12)', color: 'var(--success)' }}>
          <ArrowRightLeft size={15} />
        </div>
        <div>
          <h4 style={{ margin: 0, fontSize: 14, fontWeight: 700 }}>{t('dispatcher.backhaulTitle')}</h4>
          <p className="text-muted" style={{ margin: 0, fontSize: 12 }}>{t('dispatcher.backhaulDesc')}</p>
        </div>
      </div>

      <div className="grid grid-2 mt-16" style={{ gap: 12 }}>
        {matches.map(({ trip, nearby }) => (
          <div key={trip.tripId} style={{ border: '1px solid #e5e7eb', borderRadius: 10, padding: 14 }}>
            <div className="flex items-center gap-1.5" style={{ fontSize: 12.5, fontWeight: 600 }}>
              <PackageSearch size={13} style={{ color: 'var(--success)' }} />
              {trip.vehiclePlate || t('dispatcher.tripFallback', { id: trip.tripId })} — {trip.destinationAddress || '—'}
            </div>
            <div className="mt-8" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {nearby.map((c) => (
                <div key={c.id} className="flex-between" style={{ background: '#f9fafb', borderRadius: 8, padding: '6px 10px', fontSize: 11.5 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4, minWidth: 0, overflow: 'hidden' }}>
                    <MapPin size={11} style={{ color: 'var(--success)', flexShrink: 0 }} />
                    <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {c.trackingNumber} · {c.pickupAddress || c.description}
                    </span>
                  </span>
                  <span style={{ marginLeft: 8, flexShrink: 0, fontWeight: 700, color: 'var(--success)' }}>{c.distanceKm.toFixed(0)} km</span>
                </div>
              ))}
              <button
                type="button"
                onClick={() => setCustomerModalCargo(nearby[0])}
                className="btn btn-sm"
                style={{ alignSelf: 'flex-start', gap: 5, marginTop: 2 }}
              >
                <User size={12} /> {t('dispatcher.customerInfoBtn')}
              </button>
            </div>
          </div>
        ))}
      </div>

      <CustomerInfoModal customers={customerModalCustomers} onClose={() => setCustomerModalCargo(null)} />
    </div>
  );
}
