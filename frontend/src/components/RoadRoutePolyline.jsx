import { useEffect, useState } from 'react';
import { Polyline } from 'react-leaflet';
import { fetchRoadRoute } from '../utils/geo.js';

// Draws the ACTUAL road route between waypoints (real streets/highways the
// truck will drive, via OSRM — see utils/geo.js#fetchRoadRoute), not a
// straight "hava yolu" line cutting across buildings/rivers/fields. Used by
// every pickup->destination route line on the site (tracking map, dispatcher
// pending-loads map, driver current-trip map) so they all show real
// navigation-style routing.
//
// Renders the plain straight line between the same points immediately (so
// the map is never empty), then swaps it for the real road geometry once
// OSRM responds. If OSRM fails or can't find a drivable path (offline, rate
// limited, or the two points aren't road-connected — e.g. across open sea),
// it just quietly keeps the straight line, so the route is never missing.
export default function RoadRoutePolyline({ points, pathOptions, onRoute }) {
  const key = points && points.length >= 2 ? JSON.stringify(points) : null;
  const [roadPoints, setRoadPoints] = useState(null);

  useEffect(() => {
    setRoadPoints(null);
    if (!key) return undefined;
    let cancelled = false;
    fetchRoadRoute(points).then((route) => {
      if (cancelled) return;
      if (route) {
        setRoadPoints(route.points);
        onRoute?.(route);
      } else {
        onRoute?.(null);
      }
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  if (!points || points.length < 2) return null;
  return <Polyline positions={roadPoints || points} pathOptions={pathOptions} />;
}
