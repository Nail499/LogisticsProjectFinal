import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { History, Search, FileDown } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import { downloadCsv } from '../../utils/csvExport.js';

const ACTION_KEY = {
  DISPATCHER_CREATED: { key: 'actionDispatcherCreated', className: 'badge-success' },
  DISPATCHER_DELETED: { key: 'actionDispatcherDeleted', className: 'badge-danger' },
  DRIVER_PASSWORD_RESET: { key: 'actionDriverPasswordReset', className: 'badge-warning' },
  DRIVER_DELETED: { key: 'actionDriverDeleted', className: 'badge-danger' },
  WAREHOUSE_CREATED: { key: 'actionWarehouseCreated', className: 'badge-success' },
  WAREHOUSE_UPDATED: { key: 'actionWarehouseUpdated', className: 'badge-warning' },
  WAREHOUSE_DELETED: { key: 'actionWarehouseDeleted', className: 'badge-danger' },
  VEHICLE_CREATED: { key: 'actionVehicleCreated', className: 'badge-success' },
  VEHICLE_DELETED: { key: 'actionVehicleDeleted', className: 'badge-danger' },
  CUSTOMS_TARIFF_UPDATED: { key: 'actionCustomsTariffUpdated', className: 'badge-warning' },
  CUSTOMS_TARIFF_DELETED: { key: 'actionCustomsTariffDeleted', className: 'badge-danger' },
};

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('az-AZ', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('az-AZ', { hour: '2-digit', minute: '2-digit' });
}

// Admin "Fəaliyyət tarixçəsi" — kim, nə vaxt, hansı əməliyyatı etdi (bax
// AuditLogService, AdminManagementController-dəki çağırış nöqtələri).
// Son 200 qeyd göstərilir (bax AuditLogRepository#findTop200...).
export default function AdminAuditLog() {
  const { t } = useTranslation();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    axiosClient.get('/api/admin/audit-logs')
      .then((res) => setLogs(res.data))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return logs;
    const q = search.trim().toLowerCase();
    return logs.filter((l) =>
      l.actorUsername?.toLowerCase().includes(q) ||
      l.details?.toLowerCase().includes(q) ||
      l.action?.toLowerCase().includes(q)
    );
  }, [logs, search]);

  if (loading) return <p>{t('common.loading')}</p>;

  return (
    <div>
      <div className="flex-between">
        <div>
          <h2>{t('admin.auditTitle')}</h2>
          <p>{t('admin.auditDesc')}</p>
        </div>
        <button
          type="button"
          className="btn btn-sm flex items-center gap-1.5"
          onClick={() => downloadCsv('/api/admin/export/audit-logs.csv', 'fealiyyet-tarixcesi.csv')}
        >
          <FileDown size={14} /> {t('admin.exportBtn')}
        </button>
      </div>

      <div className="search-input-wrap mt-16" style={{ maxWidth: 360 }}>
        <input
          className="input"
          style={{ paddingLeft: 36 }}
          placeholder={t('admin.auditSearchPlaceholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span style={{ position: 'absolute', left: 10, top: 10, color: '#9ca3af' }}>
          <Search width={16} height={16} />
        </span>
      </div>

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('admin.colTime')}</th>
              <th>{t('admin.colUser')}</th>
              <th>{t('admin.colAction')}</th>
              <th>{t('admin.colDetails')}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((l) => (
              <tr key={l.id}>
                <td style={{ fontSize: 12.5, whiteSpace: 'nowrap' }}>{formatDate(l.createdAt)}</td>
                <td>
                  <div style={{ fontSize: 13 }}>{l.actorUsername}</div>
                  {l.actorRole && <div className="text-muted" style={{ fontSize: 11 }}>{l.actorRole}</div>}
                </td>
                <td>
                  <span className={`badge ${ACTION_KEY[l.action]?.className || 'badge-warning'}`}>
                    {ACTION_KEY[l.action] ? t(`admin.${ACTION_KEY[l.action].key}`) : l.action}
                  </span>
                </td>
                <td style={{ fontSize: 12.5, color: '#374151' }}>{l.details}</td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={4} className="text-center text-muted">
                  <History size={28} style={{ opacity: 0.4, margin: '8px auto', display: 'block' }} />
                  {t('admin.noLogs')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
