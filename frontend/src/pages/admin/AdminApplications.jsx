import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FileText } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const STATUS_KEY = {
  PENDING: { key: 'statusPending', ns: 'dispatcher', className: 'badge-warning' },
  APPROVED: { key: 'statusApproved', ns: 'admin', className: 'badge-success' },
  REJECTED: { key: 'statusRejected', ns: 'admin', className: 'badge-danger' },
};

export default function AdminApplications() {
  const { t } = useTranslation();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [credentials, setCredentials] = useState(null);

  const load = () => {
    setLoading(true);
    axiosClient.get('/api/admin/applications')
      .then((res) => setApplications(res.data))
      .catch(() => setError(t('admin.errLoadApplications')))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleApprove = async (id) => {
    if (!window.confirm(t('admin.approveConfirm'))) return;
    try {
      const res = await axiosClient.post(`/api/admin/applications/${id}/approve`);
      setCredentials(res.data);
      load();
    } catch (err) {
      alert(t('admin.errApprove'));
    }
  };

  const handleReject = async (id) => {
    const reason = window.prompt(t('admin.rejectReasonPrompt'));
    if (!reason) return;
    try {
      await axiosClient.post(`/api/admin/applications/${id}/reject`, { rejectionReason: reason });
      load();
    } catch (err) {
      alert(t('admin.errReject'));
    }
  };

  if (loading) return <p>{t('common.loading')}</p>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      <h2>{t('admin.applicationsTitle')}</h2>
      <p>{t('admin.applicationsDesc')}</p>

      {credentials && (
        <div className="alert alert-success">
          {t('admin.accountCreatedNotice', { username: credentials.username, password: credentials.temporaryPassword })}
          <button className="btn btn-sm" style={{ marginLeft: 12 }} onClick={() => setCredentials(null)}>{t('common.close')}</button>
        </div>
      )}

      <div className="table-wrap mt-16">
        <table>
          <thead>
            <tr>
              <th>{t('admin.colId')}</th>
              <th>{t('admin.colFullName')}</th>
              <th>{t('admin.colPhone')}</th>
              <th>{t('admin.colPlate')}</th>
              <th>{t('admin.colBrand')}</th>
              <th>{t('admin.colDocuments')}</th>
              <th>{t('dispatcher.colStatus')}</th>
              <th>{t('admin.colAction')}</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((app) => (
              <tr key={app.id}>
                <td>{app.id}</td>
                <td>{app.fullName}</td>
                <td>{app.phone}</td>
                {app.hasOwnVehicle ? (
                  <>
                    <td>{app.vehiclePlateNumber}</td>
                    <td>{app.vehicleBrand}</td>
                  </>
                ) : (
                  <td colSpan={2} className="text-muted" style={{ fontSize: 12.5, fontStyle: 'italic' }}>
                    {t('admin.noOwnVehicleNote')}
                  </td>
                )}
                <td>
                  <div className="flex gap-8" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {app.licenseDocumentUrl ? (
                      <a href={API_BASE + app.licenseDocumentUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12.5 }}>
                        <FileText size={13} /> {t('admin.licenseDoc')}
                      </a>
                    ) : (
                      <span className="text-muted" style={{ fontSize: 12.5 }}>{t('admin.noLicenseDoc')}</span>
                    )}
                    {app.vehicleDocumentUrl ? (
                      <a href={API_BASE + app.vehicleDocumentUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12.5 }}>
                        <FileText size={13} /> {t('admin.vehicleDoc')}
                      </a>
                    ) : (
                      <span className="text-muted" style={{ fontSize: 12.5 }}>{t('admin.noVehicleDoc')}</span>
                    )}
                  </div>
                </td>
                <td>
                  <span className={`badge ${STATUS_KEY[app.status]?.className}`}>
                    {STATUS_KEY[app.status] ? t(`${STATUS_KEY[app.status].ns}.${STATUS_KEY[app.status].key}`) : app.status}
                  </span>
                </td>
                <td>
                  {app.status === 'PENDING' ? (
                    <div className="flex gap-8">
                      <button className="btn btn-sm btn-primary" onClick={() => handleApprove(app.id)}>{t('admin.approveBtn')}</button>
                      <button className="btn btn-sm btn-danger" onClick={() => handleReject(app.id)}>{t('admin.rejectBtn')}</button>
                    </div>
                  ) : (
                    <span className="text-muted" style={{ fontSize: 13 }}>{t('admin.reviewedLabel')}</span>
                  )}
                </td>
              </tr>
            ))}
            {applications.length === 0 && (
              <tr><td colSpan={9} className="text-center text-muted">{t('admin.noApplications')}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
