export default function TruckHero() {
  return (
    <svg
      className="hero-illustration"
      viewBox="0 0 820 480"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="Real gorunuslu tir illustrasiyasi"
    >
      <defs>
        <linearGradient id="skyGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#0b1224" />
          <stop offset="0.45" stopColor="#122250" />
          <stop offset="0.75" stopColor="#1e3a8a" />
          <stop offset="1" stopColor="#3b5fc4" />
        </linearGradient>
        <radialGradient id="sunGlow" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#fef3c7" stopOpacity="0.95" />
          <stop offset="0.35" stopColor="#fbbf24" stopOpacity="0.5" />
          <stop offset="1" stopColor="#fbbf24" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="cabPaint" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#bfdbfe" />
          <stop offset="0.25" stopColor="#60a5fa" />
          <stop offset="0.55" stopColor="#2563eb" />
          <stop offset="1" stopColor="#1741a6" />
        </linearGradient>
        <linearGradient id="cabHighlight" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#fff" stopOpacity="0.55" />
          <stop offset="0.5" stopColor="#fff" stopOpacity="0" />
        </linearGradient>
        <linearGradient id="trailerPaint" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="0.6" stopColor="#eef1f6" />
          <stop offset="1" stopColor="#c9d2e0" />
        </linearGradient>
        <radialGradient id="chromeRing" cx="0.35" cy="0.3" r="0.75">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="0.35" stopColor="#cbd5e1" />
          <stop offset="0.7" stopColor="#64748b" />
          <stop offset="1" stopColor="#0f172a" />
        </radialGradient>
        <radialGradient id="headlampGlow" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#fffbea" stopOpacity="0.95" />
          <stop offset="1" stopColor="#fffbea" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="beamGrad" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#fef9c3" stopOpacity="0.55" />
          <stop offset="1" stopColor="#fef9c3" stopOpacity="0" />
        </linearGradient>
        <radialGradient id="tireGrad" cx="0.4" cy="0.35" r="0.7">
          <stop offset="0" stopColor="#4b5563" />
          <stop offset="0.55" stopColor="#1f2937" />
          <stop offset="1" stopColor="#020617" />
        </radialGradient>
        <linearGradient id="asphalt" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#374151" />
          <stop offset="1" stopColor="#0f172a" />
        </linearGradient>
        <linearGradient id="hazardStripe" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#f59e0b" />
          <stop offset="0.5" stopColor="#f59e0b" />
          <stop offset="0.5" stopColor="#111827" />
          <stop offset="1" stopColor="#111827" />
        </linearGradient>
        <filter id="softShadow" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="6" />
        </filter>
        <filter id="lightBlur" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3" />
        </filter>
      </defs>

      {/* sky */}
      <rect x="0" y="0" width="820" height="330" fill="url(#skyGrad)" />
      <circle className="sun-pulse" cx="670" cy="90" r="120" fill="url(#sunGlow)" />
      <circle cx="670" cy="90" r="26" fill="#fef3c7" opacity="0.9" />

      {/* stars */}
      <g opacity="0.7">
        <circle cx="60" cy="40" r="1.4" fill="#fff" />
        <circle cx="130" cy="70" r="1" fill="#fff" />
        <circle cx="220" cy="35" r="1.6" fill="#fff" />
        <circle cx="310" cy="60" r="1" fill="#fff" />
        <circle cx="400" cy="30" r="1.3" fill="#fff" />
        <circle cx="480" cy="55" r="1" fill="#fff" />
      </g>

      {/* mountains / skyline layers */}
      <polygon points="0,260 90,190 180,260" fill="#0b1739" opacity="0.55" />
      <polygon points="140,260 260,175 380,260" fill="#0e1c46" opacity="0.55" />
      <polygon points="330,260 430,205 540,260" fill="#0b1739" opacity="0.45" />
      <g opacity="0.5">
        <rect x="600" y="205" width="18" height="60" fill="#1e3a8a" />
        <rect x="626" y="180" width="14" height="85" fill="#1e3a8a" />
        <rect x="648" y="215" width="16" height="50" fill="#1e3a8a" />
        <rect x="760" y="195" width="16" height="70" fill="#1e3a8a" />
        <rect x="784" y="220" width="14" height="45" fill="#1e3a8a" />
      </g>

      {/* road */}
      <rect x="0" y="330" width="820" height="150" fill="url(#asphalt)" />
      <rect x="0" y="326" width="820" height="6" fill="#475569" />
      <rect x="0" y="326" width="820" height="2" fill="#94a3b8" opacity="0.6" />
      <g opacity="0.08">
        <line x1="0" y1="360" x2="820" y2="345" stroke="#fff" strokeWidth="2" />
        <line x1="0" y1="410" x2="820" y2="390" stroke="#fff" strokeWidth="2" />
        <line x1="0" y1="455" x2="820" y2="430" stroke="#fff" strokeWidth="2" />
      </g>
      <g className="road-dashes">
        <rect x="0" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="90" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="180" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="270" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="360" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="450" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="540" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="630" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="720" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
        <rect x="810" y="382" width="50" height="7" rx="3.5" fill="#facc15" />
      </g>

      {/* floating cargo badges */}
      <g className="float-slow">
        <rect x="55" y="70" width="46" height="46" rx="6" fill="#f59e0b" />
        <path d="M55 88h46M78 70v46" stroke="#92400e" strokeWidth="2" />
      </g>
      <g className="float-slower">
        <rect x="700" y="250" width="34" height="34" rx="6" fill="#22c55e" />
        <path d="M700 267h34M717 250v34" stroke="#14532d" strokeWidth="2" />
      </g>

      {/* location pin */}
      <g className="bounce-pin">
        <path d="M620 175c0 20-22 42-22 42s-22-22-22-42a22 22 0 1 1 44 0z" fill="#ef4444" />
        <circle cx="598" cy="175" r="8.5" fill="white" />
      </g>

      {/* motion streaks */}
      <g className="truck-drive" opacity="0.55">
        <rect x="45" y="270" width="34" height="5" rx="2.5" fill="#93c5fd" filter="url(#lightBlur)" />
        <rect x="20" y="292" width="46" height="5" rx="2.5" fill="#93c5fd" filter="url(#lightBlur)" />
        <rect x="55" y="314" width="26" height="5" rx="2.5" fill="#93c5fd" filter="url(#lightBlur)" />
      </g>

      {/* ================= TRUCK ================= */}
      <g className="truck-drive">
        {/* ground shadow */}
        <ellipse cx="330" cy="392" rx="230" ry="15" fill="black" opacity="0.35" filter="url(#softShadow)" />

        {/* headlight beam */}
        <polygon points="460,318 640,300 640,345 460,340" fill="url(#beamGrad)" className="beam-pulse" />

        {/* trailer */}
        <rect x="110" y="205" width="260" height="160" rx="10" fill="url(#trailerPaint)" stroke="#aab4c4" strokeWidth="2" />
        <rect x="110" y="205" width="260" height="26" rx="10" fill="#dde3ee" />
        {/* corrugated panel lines */}
        <g stroke="#c7cfdc" strokeWidth="1">
          <line x1="130" y1="231" x2="130" y2="365" />
          <line x1="152" y1="231" x2="152" y2="365" />
          <line x1="174" y1="231" x2="174" y2="365" />
          <line x1="196" y1="231" x2="196" y2="365" />
          <line x1="218" y1="231" x2="218" y2="365" />
          <line x1="240" y1="231" x2="240" y2="365" />
          <line x1="262" y1="231" x2="262" y2="365" />
          <line x1="284" y1="231" x2="284" y2="365" />
          <line x1="306" y1="231" x2="306" y2="365" />
          <line x1="328" y1="231" x2="328" y2="365" />
          <line x1="350" y1="231" x2="350" y2="365" />
        </g>
        {/* brand plate */}
        <rect x="140" y="255" width="200" height="60" rx="8" fill="#2563eb" opacity="0.07" />
        <text x="240" y="292" textAnchor="middle" fontSize="30" fontWeight="800" fill="#1d4ed8" fontFamily="Segoe UI, Arial, sans-serif">
          Fleet<tspan fill="#22FFB0">ra</tspan>
        </text>
        <line x1="110" y1="225" x2="110" y2="365" stroke="#aab4c4" strokeWidth="2" />
        {/* rear tail light */}
        <rect x="114" y="335" width="12" height="18" rx="3" fill="#ef4444" />
        <rect x="114" y="335" width="12" height="18" rx="3" fill="#fff" opacity="0.2" />
        {/* hazard stripe bottom */}
        <rect x="110" y="352" width="260" height="8" fill="url(#hazardStripe)" opacity="0.9" />
        {/* under-ride guard */}
        <rect x="118" y="362" width="244" height="6" rx="2" fill="#111827" />

        {/* cab */}
        <path d="M368 235h78l58 56v56a9 9 0 0 1-9 9H368V235z" fill="url(#cabPaint)" stroke="#14329e" strokeWidth="1.5" />
        <path d="M368 235h78l58 56v56a9 9 0 0 1-9 9H368V235z" fill="url(#cabHighlight)" opacity="0.5" />
        {/* roof deflector */}
        <path d="M368 235h78l14 15h-92z" fill="#0f2a86" opacity="0.55" />
        {/* windshield with reflection */}
        <path d="M384 253h34l28 30h-62v-30z" fill="#dbeeff" opacity="0.96" />
        <path d="M384 253h34l28 30h-62v-30z" fill="none" stroke="#14329e" strokeWidth="1.5" />
        <polygon points="388,255 405,255 392,275" fill="#ffffff" opacity="0.55" />
        {/* side window */}
        <rect x="440" y="270" width="16" height="16" rx="2" fill="#dbeeff" opacity="0.8" />
        {/* side mirror */}
        <rect x="356" y="256" width="7" height="18" rx="3" fill="#0f172a" />
        <rect x="356" y="256" width="3" height="18" rx="1.5" fill="#94a3b8" opacity="0.7" />
        {/* door + handle + decal */}
        <line x1="410" y1="283" x2="410" y2="337" stroke="#14329e" strokeWidth="1.2" opacity="0.55" />
        <rect x="417" y="305" width="10" height="3.5" rx="1.5" fill="#0f2a86" />
        <circle cx="412" cy="318" r="9" fill="#f59e0b" opacity="0.9" />
        <text x="412" y="322" textAnchor="middle" fontSize="9" fontWeight="700" fill="#0f172a">TL</text>
        {/* fuel tank */}
        <rect x="378" y="330" width="46" height="20" rx="8" fill="#334155" />
        <rect x="378" y="330" width="46" height="6" rx="3" fill="#64748b" opacity="0.6" />
        {/* exhaust stack */}
        <rect x="362" y="238" width="9" height="55" rx="3" fill="url(#chromeRing)" />
        <ellipse cx="366.5" cy="238" rx="5.5" ry="3" fill="#e2e8f0" />
        <g className="float-slower" opacity="0.55">
          <circle cx="366" cy="228" r="6" fill="#cbd5e1" />
          <circle cx="358" cy="216" r="8" fill="#cbd5e1" opacity="0.7" />
          <circle cx="368" cy="204" r="10" fill="#cbd5e1" opacity="0.45" />
        </g>
        {/* grille + chrome bumper */}
        <rect x="430" y="300" width="24" height="24" rx="4" fill="#111827" />
        <g stroke="#475569" strokeWidth="1.4">
          <line x1="432" y1="305" x2="452" y2="305" />
          <line x1="432" y1="310" x2="452" y2="310" />
          <line x1="432" y1="315" x2="452" y2="315" />
          <line x1="432" y1="320" x2="452" y2="320" />
        </g>
        <rect x="426" y="326" width="34" height="10" rx="3" fill="url(#chromeRing)" />
        {/* headlamp */}
        <circle cx="452" cy="313" r="17" fill="url(#headlampGlow)" />
        <circle cx="452" cy="313" r="9.5" fill="#fef9c3" stroke="#eab308" strokeWidth="1.2" />
        <circle cx="449" cy="310" r="3" fill="#fff" opacity="0.9" />
      </g>

      {/* wheels drawn outside truck-drive group offset so they sit on the road consistently */}
      <g className="truck-drive">
        {[190, 300, 415].map((cx, i) => (
          <g key={cx}>
            <circle cx={cx} cy="372" r="28" fill="url(#tireGrad)" />
            {Array.from({ length: 14 }).map((_, t) => {
              const angle = (t / 14) * Math.PI * 2;
              const x1 = cx + Math.cos(angle) * 24;
              const y1 = 372 + Math.sin(angle) * 24;
              const x2 = cx + Math.cos(angle) * 28;
              const y2 = 372 + Math.sin(angle) * 28;
              return <line key={t} x1={x1} y1={y1} x2={x2} y2={y2} stroke="#111827" strokeWidth="2.4" />;
            })}
            <circle cx={cx} cy="372" r="13" fill="url(#chromeRing)" />
            <circle cx={cx} cy="372" r="5" fill="#1f2937" />
            <circle cx={cx - 4} cy="368" r="2.2" fill="#fff" opacity="0.6" />
          </g>
        ))}
      </g>
    </svg>
  );
}
