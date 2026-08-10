// Control Tower xəritəsi üçün "tır/sürücü axtarışı" overlay-i. MapSearchBox.jsx
// (ünvan axtarışı) ilə eyni vizual dil və useMap()/flyTo naxışını izləyir, amma
// geocoding əvəzinə birbaşa client-side liveTrips massivini süzür — sürücünün
// adı, telefonu, tırın (vehiclePlate) və ya qoşqunun (trailerPlate) nömrəsi
// üzrə. Yalnız xəritədə görünən (koordinatı olan) reyslər axtarıla bilər;
// digərləri üçün xəritədə göstərəcək heç nə yoxdur.
import { useEffect, useMemo, useRef, useState } from 'react';
import { useMap } from 'react-leaflet';
import { useTranslation } from 'react-i18next';
import L from 'leaflet';
import { Search, X, Truck } from 'lucide-react';

export default function TripSearchBox({ trips, onSelect }) {
  const { t } = useTranslation();
  const map = useMap();
  const boxRef = useRef(null);
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!boxRef.current) return;
    L.DomEvent.disableClickPropagation(boxRef.current);
    L.DomEvent.disableScrollPropagation(boxRef.current);
  }, []);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (q.length < 2) return [];
    return trips
      .filter((tr) =>
        tr.driverName?.toLowerCase().includes(q) ||
        tr.driverPhone?.toLowerCase().includes(q) ||
        tr.vehiclePlate?.toLowerCase().includes(q) ||
        tr.trailerPlate?.toLowerCase().includes(q)
      )
      .slice(0, 8);
  }, [query, trips]);

  const pick = (trip) => {
    map.flyTo([trip.lastLatitude, trip.lastLongitude], 13, { duration: 0.8 });
    onSelect(trip);
    setQuery('');
    setOpen(false);
  };

  const clear = () => {
    setQuery('');
    setOpen(false);
  };

  return (
    <div ref={boxRef} style={{ position: 'absolute', top: 10, left: 10, right: 10, zIndex: 1000, maxWidth: 340 }}>
      <div style={{ position: 'relative' }}>
        <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: '#9ca3af', pointerEvents: 'none' }} />
        <input
          value={query}
          onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
          onFocus={() => query.trim().length >= 2 && setOpen(true)}
          placeholder={t('dispatcher.tripSearchPlaceholder')}
          style={{
            width: '100%',
            padding: '9px 30px 9px 30px',
            borderRadius: 8,
            border: '1px solid #e5e7eb',
            background: '#fff',
            fontSize: 13,
            color: '#111827',
            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
            outline: 'none',
          }}
        />
        {query && (
          <button
            type="button"
            onClick={clear}
            style={{ position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)', border: 'none', background: 'none', cursor: 'pointer', color: '#9ca3af', display: 'flex' }}
          >
            <X size={14} />
          </button>
        )}
      </div>

      {open && query.trim().length >= 2 && (
        <div
          style={{
            marginTop: 4,
            background: '#fff',
            borderRadius: 8,
            border: '1px solid #e5e7eb',
            boxShadow: '0 4px 14px rgba(0,0,0,0.14)',
            maxHeight: 240,
            overflowY: 'auto',
          }}
        >
          {results.length === 0 && (
            <div style={{ padding: '10px 12px', fontSize: 12.5, color: '#9ca3af' }}>{t('dispatcher.tripSearchNoResults')}</div>
          )}
          {results.map((tr) => (
            <div
              key={tr.tripId}
              onClick={() => pick(tr)}
              style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '9px 12px', fontSize: 12.5, color: '#374151', cursor: 'pointer', borderTop: '1px solid #f3f4f6' }}
              onMouseEnter={(e) => { e.currentTarget.style.background = '#f9fafb'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; }}
            >
              <Truck size={13} style={{ color: 'var(--primary)', flexShrink: 0 }} />
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {tr.vehiclePlate || `#${tr.tripId}`}{tr.trailerPlate ? ` + ${tr.trailerPlate}` : ''}
                </div>
                <div className="text-muted" style={{ fontSize: 11, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {tr.driverName || '—'}{tr.driverPhone ? ` · ${tr.driverPhone}` : ''}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
