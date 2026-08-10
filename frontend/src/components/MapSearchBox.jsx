import { useEffect, useRef, useState } from 'react';
import { useMap } from 'react-leaflet';
import { useTranslation } from 'react-i18next';
import L from 'leaflet';
import { Search, X } from 'lucide-react';
import { forwardGeocode } from '../utils/geo.js';

// Reusable "search for an address" overlay for Leaflet selection maps
// (customer/dispatcher new-order pickup & destination pick, admin warehouse
// placement). Must be rendered as a CHILD of <MapContainer> — it calls
// useMap() to fly the view to the chosen result. It's a plain positioned
// <div>, not a real Leaflet control, which is enough since it only needs to
// sit visually on top of the tiles.
export default function MapSearchBox({ onSelect, placeholder }) {
  const { t } = useTranslation();
  const map = useMap();
  const boxRef = useRef(null);
  const debounceRef = useRef(null);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);

  // Clicks/scrolls/double-clicks inside the search box must NOT reach the
  // map underneath (typing a space, or scrolling the results list, would
  // otherwise pan/zoom the map). L.DomEvent.disable*Propagation is the
  // standard Leaflet way to do this — plain React stopPropagation only
  // stops React's synthetic bubbling, not Leaflet's own native listeners
  // attached directly to the map container.
  useEffect(() => {
    if (!boxRef.current) return;
    L.DomEvent.disableClickPropagation(boxRef.current);
    L.DomEvent.disableScrollPropagation(boxRef.current);
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (query.trim().length < 3) {
      setResults([]);
      setLoading(false);
      return undefined;
    }
    setLoading(true);
    debounceRef.current = setTimeout(async () => {
      const found = await forwardGeocode(query);
      setResults(found);
      setLoading(false);
      setOpen(true);
    }, 450);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  const pick = (r) => {
    map.flyTo([r.lat, r.lng], 15, { duration: 0.8 });
    onSelect(r.lat, r.lng, r.label);
    setQuery(r.label);
    setOpen(false);
    setResults([]);
  };

  const clear = () => {
    setQuery('');
    setResults([]);
    setOpen(false);
  };

  return (
    <div ref={boxRef} style={{ position: 'absolute', top: 10, left: 10, right: 10, zIndex: 1000, maxWidth: 340 }}>
      <div style={{ position: 'relative' }}>
        <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: '#9ca3af', pointerEvents: 'none' }} />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          placeholder={placeholder || t('map.defaultSearchPlaceholder')}
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

      {open && (loading || results.length > 0 || query.trim().length >= 3) && (
        <div
          style={{
            marginTop: 4,
            background: '#fff',
            borderRadius: 8,
            border: '1px solid #e5e7eb',
            boxShadow: '0 4px 14px rgba(0,0,0,0.14)',
            maxHeight: 220,
            overflowY: 'auto',
          }}
        >
          {loading && <div style={{ padding: '10px 12px', fontSize: 12.5, color: '#9ca3af' }}>{t('map.searching')}</div>}
          {!loading && results.map((r, i) => (
            <div
              key={`${r.lat}-${r.lng}-${i}`}
              onClick={() => pick(r)}
              style={{
                padding: '9px 12px',
                fontSize: 12.5,
                color: '#374151',
                cursor: 'pointer',
                borderTop: i > 0 ? '1px solid #f3f4f6' : 'none',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = '#f9fafb'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; }}
            >
              {r.label}
            </div>
          ))}
          {!loading && results.length === 0 && (
            <div style={{ padding: '10px 12px', fontSize: 12.5, color: '#9ca3af' }}>{t('map.noResultsFound')}</div>
          )}
        </div>
      )}
    </div>
  );
}
