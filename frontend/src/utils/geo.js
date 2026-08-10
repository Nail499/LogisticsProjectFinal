// Shared great-circle distance helper (Stage 4). Used client-side for the
// Control Tower's "closest 3 warehouses" lookup and the backhaul/empty-miles
// matcher, mirroring the same haversine formula the backend uses for ETA
// (see PublicTrackingController#haversineKm) so the numbers stay consistent.
export function haversineKm(lat1, lon1, lat2, lon2) {
  if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Infinity;
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Real road networks are typically 20-40% longer than straight-line ("hava
// yolu" / as-the-crow-flies) distance outside dense urban cores — mirrors
// the backend's RouteEstimationService (same 1.3x factor) so any "km" shown
// to a dispatcher/driver/customer reflects an approximate road distance,
// not the shorter straight-line one.
export const ROAD_DISTANCE_FACTOR = 1.3;

export function roadDistanceKm(lat1, lon1, lat2, lon2) {
  const air = haversineKm(lat1, lon1, lat2, lon2);
  return Number.isFinite(air) ? air * ROAD_DISTANCE_FACTOR : air;
}

// Returns the `count` closest items from `points` (each needs .latitude/.longitude)
// to the given [lat, lng], each annotated with a `distanceKm` field (road-
// distance estimate, not straight-line — see roadDistanceKm above).
export function closestByDistance(fromLat, fromLng, points, count = 3, getCoords = (p) => [p.latitude, p.longitude]) {
  return points
    .map((p) => {
      const [lat, lng] = getCoords(p);
      return { ...p, distanceKm: roadDistanceKm(fromLat, fromLng, lat, lng) };
    })
    .filter((p) => Number.isFinite(p.distanceKm))
    .sort((a, b) => a.distanceKm - b.distanceKm)
    .slice(0, count);
}

// Forward-geocodes free-text address search into candidate lat/lng points,
// used by MapSearchBox (address search box overlaid on selection maps —
// new-order pickup/destination, admin warehouse placement) so the user can
// type an address instead of hunting for it by eye/clicking on the map.
// Same OSM Nominatim provider as reverseGeocode below, so no new vendor.
export async function forwardGeocode(query) {
  const q = (query || '').trim();
  if (q.length < 3) return [];
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(q)}&addressdetails=1&limit=6&accept-language=az`,
      { headers: { Accept: 'application/json' } },
    );
    if (!res.ok) throw new Error('forward geocode failed');
    const data = await res.json();
    return data.map((item) => ({
      lat: parseFloat(item.lat),
      lng: parseFloat(item.lon),
      label: item.display_name,
    })).filter((r) => Number.isFinite(r.lat) && Number.isFinite(r.lng));
  } catch {
    return [];
  }
}

// Fetches the actual road-following route (turn-by-turn geometry, following
// real streets/highways — not a straight "hava yolu" line) between an
// ordered list of [lat, lng] waypoints, via the public OSRM demo routing
// server (router.project-osrm.org). Same "call it straight from the
// browser, no API key, no backend involvement" pattern already used for
// forwardGeocode/reverseGeocode above (OSM-family service). Returns null on
// any failure (offline, rate-limited, or no drivable road between the
// points — e.g. across open sea) so callers can fall back to drawing the
// old straight line instead of showing nothing.
export async function fetchRoadRoute(points) {
  if (!points || points.length < 2) return null;
  const coordsParam = points.map(([lat, lng]) => `${lng},${lat}`).join(';');
  try {
    const res = await fetch(
      `https://router.project-osrm.org/route/v1/driving/${coordsParam}?overview=full&geometries=geojson`,
    );
    if (!res.ok) throw new Error('routing failed');
    const data = await res.json();
    const route = data.routes?.[0];
    const coords = route?.geometry?.coordinates;
    if (!coords || coords.length < 2) return null;
    return {
      // GeoJSON is [lng, lat] — Leaflet wants [lat, lng].
      points: coords.map(([lng, lat]) => [lat, lng]),
      distanceKm: route.distance / 1000,
      durationMin: route.duration / 60,
      // One entry per leg between consecutive waypoints — e.g. for
      // [pickup, live, destination] waypoints, legs[0] is the pickup->live
      // distance/time already covered and legs[1] is what's left to
      // destination. Used by LiveTrackingPanel to split "qət edilib" /
      // "qalıb" instead of just showing the total.
      legs: (route.legs || []).map((leg) => ({
        distanceKm: leg.distance / 1000,
        durationMin: leg.duration / 60,
      })),
    };
  } catch {
    return null;
  }
}

export async function reverseGeocode(lat, lng) {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&addressdetails=1&accept-language=az`,
      { headers: { Accept: 'application/json' } },
    );
    if (!res.ok) throw new Error('reverse geocode failed');
    const data = await res.json();
    const a = data.address || {};
    const road = a.road || a.pedestrian || a.footway;
    const area = a.suburb || a.neighbourhood || a.city_district;
    const city = a.city || a.town || a.village || a.county;

    const parts = [];
    if (road) parts.push(a.house_number ? `${road} ${a.house_number}` : road);
    else if (area) parts.push(area);
    if (city && city !== road) parts.push(city);

    if (parts.length) return parts.join(', ');
    return data.display_name || `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
  } catch {
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
  }
}
