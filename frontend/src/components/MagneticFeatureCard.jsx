// Premium feature card with a "magnetic" hover effect: the card subtly
// leans toward the cursor and a soft radial spotlight follows the mouse,
// finished off with a glowing accent border — the Vercel/Linear-style
// micro-interaction used across the Stage 2 features grid.
import { useRef, useState } from 'react';

export default function MagneticFeatureCard({ icon: Icon, title, desc, accent = 'blue' }) {
  const ref = useRef(null);
  const [style, setStyle] = useState({});
  const [spot, setSpot] = useState({ x: 50, y: 50, active: false });

  const accentClasses = {
    blue: 'text-accent-blue bg-accent-blue/10',
    green: 'text-neon-green bg-neon-green/10',
    red: 'text-anomaly-red bg-anomaly-red/10',
  }[accent];

  const handleMove = (e) => {
    const el = ref.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const px = ((e.clientX - rect.left) / rect.width) * 100;
    const py = ((e.clientY - rect.top) / rect.height) * 100;
    const dx = (e.clientX - rect.left - rect.width / 2) / (rect.width / 2);
    const dy = (e.clientY - rect.top - rect.height / 2) / (rect.height / 2);
    setStyle({ transform: `translate(${dx * 6}px, ${dy * 6}px)` });
    setSpot({ x: px, y: py, active: true });
  };

  const handleLeave = () => {
    setStyle({ transform: 'translate(0, 0)' });
    setSpot((s) => ({ ...s, active: false }));
  };

  return (
    <div
      ref={ref}
      onMouseMove={handleMove}
      onMouseLeave={handleLeave}
      style={style}
      className="group relative overflow-hidden rounded-xl border border-base-700 bg-base-900/60 p-7 transition-[transform,box-shadow] duration-200 ease-out hover:border-accent-blue/50 hover:shadow-glow"
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        style={{
          background: spot.active
            ? `radial-gradient(220px circle at ${spot.x}% ${spot.y}%, rgba(59,130,246,0.12), transparent 70%)`
            : 'none',
        }}
      />
      <div className={`relative mb-5 flex h-12 w-12 items-center justify-center rounded-xl ${accentClasses}`}>
        <Icon size={22} />
      </div>
      <h3 className="relative font-heading text-base font-bold text-white">{title}</h3>
      <p className="relative mt-2 text-sm leading-relaxed text-slate-400">{desc}</p>
    </div>
  );
}
