// Stage 4 — Recharts widgets: monthly expense totals + an estimated
// monthly carbon footprint. Data comes pre-aggregated from
// /api/dispatcher/reports/analytics (see AdminReportService#getMonthlyAnalytics);
// the carbon numbers are a documented estimate (distance x emission factor),
// not measured telemetry — labelled as such in the UI.
// Restyled to the site's light Fleetra theme.
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { BarChart3, Leaf } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const tooltipStyle = {
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 10,
  fontSize: 12,
  color: '#111827',
  boxShadow: '0 2px 10px rgba(0,0,0,0.08)',
};

export default function AnalyticsCharts() {
  const { t } = useTranslation();
  const [data, setData] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/dispatcher/reports/analytics')
      .then((res) => setData(res.data))
      .catch(() => setData({ monthlyExpenses: [], monthlyCarbonFootprintKg: [] }));
  }, []);

  if (!data) return null;

  return (
    <div className="grid grid-2 mt-24" style={{ gap: 16 }}>
      <div className="card">
        <div className="flex items-center gap-1.5 text-xs text-muted" style={{ fontWeight: 600, textTransform: 'uppercase' }}>
          <BarChart3 size={12} style={{ color: 'var(--primary)' }} /> {t('dispatcher.expensesChartTitle')}
        </div>
        <div className="mt-8" style={{ height: 224 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.monthlyExpenses}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
              <XAxis dataKey="label" stroke="#9ca3af" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis stroke="#9ca3af" fontSize={11} tickLine={false} axisLine={false} />
              <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgba(37,99,235,0.06)' }} />
              <Bar dataKey="value" fill="#2563eb" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center gap-1.5 text-xs text-muted" style={{ fontWeight: 600, textTransform: 'uppercase' }}>
          <Leaf size={12} style={{ color: 'var(--success)' }} /> {t('dispatcher.carbonChartTitle')}
        </div>
        <p className="text-muted" style={{ margin: '2px 0 0', fontSize: 10.5 }}>{t('dispatcher.carbonChartHint')}</p>
        <div className="mt-8" style={{ height: 208 }}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data.monthlyCarbonFootprintKg}>
              <defs>
                <linearGradient id="carbonFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#16a34a" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#16a34a" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
              <XAxis dataKey="label" stroke="#9ca3af" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis stroke="#9ca3af" fontSize={11} tickLine={false} axisLine={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Area type="monotone" dataKey="value" stroke="#16a34a" strokeWidth={2} fill="url(#carbonFill)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
