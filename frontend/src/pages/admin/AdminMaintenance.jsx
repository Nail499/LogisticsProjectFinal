import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Trash2, User, KeyRound, Sparkles } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

// Bir dəfəlik "test datasını sıfırla" səhifəsi (bax MaintenanceService,
// AdminMaintenanceController). Qəsdən AdminLayout naviqasiyasına ƏLAVƏ
// OLUNMUR — yalnız birbaşa /admin/maintenance ünvanından əlçatandır ki,
// bu qədər dağıdıcı əməliyyat təsadüfən menyudan klikə məruz qalmasın.
// İkiqat təsdiq tələb olunur: "SİL" yazmaq + brauzer confirm dialoqu.
export default function AdminMaintenance() {
  const { t } = useTranslation();

  const ROLE_LABELS = { ADMIN: t('admin.roleAdmin'), DISPATCHER: t('admin.roleDispatcher'), CUSTOMER: t('admin.roleCustomer') };

  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(true);
  const [confirmText, setConfirmText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [pwOpenRole, setPwOpenRole] = useState(null);
  const [pwValue, setPwValue] = useState('');
  const [pwSubmitting, setPwSubmitting] = useState(false);
  const [pwMessage, setPwMessage] = useState({});
  const [seeding, setSeeding] = useState(false);
  const [seedResult, setSeedResult] = useState(null);
  const [seedError, setSeedError] = useState('');
  const [wipeConfirmText, setWipeConfirmText] = useState('');
  const [wiping, setWiping] = useState(false);
  const [wipeResult, setWipeResult] = useState(null);
  const [wipeError, setWipeError] = useState('');

  useEffect(() => {
    axiosClient.get('/api/admin/maintenance/preview')
      .then((res) => setPreview(res.data))
      .finally(() => setPreviewLoading(false));
  }, []);

  const canSubmit = confirmText.trim().toUpperCase() === 'SİL' || confirmText.trim().toUpperCase() === 'SIL';

  const handleReset = async () => {
    if (!canSubmit) return;
    if (!window.confirm(t('admin.resetConfirmDialog'))) {
      return;
    }
    setSubmitting(true);
    setError('');
    setResult(null);
    try {
      const res = await axiosClient.post('/api/admin/maintenance/reset-test-data');
      setResult(res.data);
      setConfirmText('');
      axiosClient.get('/api/admin/maintenance/preview').then((r) => setPreview(r.data));
    } catch (err) {
      setError(err.response?.data?.message || t('admin.errOperationFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  // Rol üzrə şifrə sıfırlama — dispetçer üçün User.id, müştəri üçün
  // Customer.id gözlənilir (bax AdminManagementController#resetDispatcherPassword/
  // resetCustomerPassword). Admin öz şifrəsini bilir (giriş edə bilir), ona
  // görə ADMIN üçün bu forma göstərilmir.
  const handleResetPassword = async (account) => {
    if (!pwValue.trim()) return;
    setPwSubmitting(true);
    setPwMessage((m) => ({ ...m, [account.role]: null }));
    try {
      const url = account.role === 'DISPATCHER'
        ? `/api/admin/dispatchers/${account.id}/password`
        : `/api/admin/customers/${account.customerId}/password`;
      await axiosClient.put(url, { newPassword: pwValue.trim() });
      setPwMessage((m) => ({ ...m, [account.role]: 'ok' }));
      setPwValue('');
      setPwOpenRole(null);
    } catch (err) {
      setPwMessage((m) => ({ ...m, [account.role]: err.response?.data?.message || t('admin.errPasswordUpdate2') }));
    } finally {
      setPwSubmitting(false);
    }
  };

  // Panel boş görsənməsin deyə 6 nümunə sürücü (bəziləri öz tırı ilə,
  // bəziləri tırsız), 3 şirkət tırı, 2 şirkət qoşqusu, sürücülərə bağlı
  // tırlar/qoşqular yaradır — bax MaintenanceService#seedDemoData. YÜK/
  // REYS yaratmır (istifadəçi bunu ayrıca istədi). Təkrar klikləmək
  // təhlükəsizdir — mövcud telefon/plaka nömrələri ötürülür.
  const handleSeedDemoData = async () => {
    setSeeding(true);
    setSeedError('');
    setSeedResult(null);
    try {
      const res = await axiosClient.post('/api/admin/maintenance/seed-demo-data');
      setSeedResult(res.data);
    } catch (err) {
      setSeedError(err.response?.data?.message || t('admin.errSeedData'));
    } finally {
      setSeeding(false);
    }
  };

  const wipeCanSubmit = wipeConfirmText.trim().toUpperCase() === 'SİL' || wipeConfirmText.trim().toUpperCase() === 'SIL';

  // Admini, sürücüləri və tırları/qoşquları TOXUNULMAZ saxlayaraq yalnız
  // (əvvəlki sıfırlamadan sonra qalan) DISPATCHER + CUSTOMER hesabını
  // silir — istifadəçi bunları özü sıfırdan yaratmaq istədi (bax
  // MaintenanceService#wipeDispatcherAndCustomerAccounts).
  const handleWipeDispatcherCustomer = async () => {
    if (!wipeCanSubmit) return;
    if (!window.confirm(t('admin.wipeConfirmDialog'))) {
      return;
    }
    setWiping(true);
    setWipeError('');
    setWipeResult(null);
    try {
      const res = await axiosClient.post('/api/admin/maintenance/wipe-dispatcher-customer');
      setWipeResult(res.data);
      setWipeConfirmText('');
      axiosClient.get('/api/admin/maintenance/preview').then((r) => setPreview(r.data));
    } catch (err) {
      setWipeError(err.response?.data?.message || t('admin.errOperationFailed'));
    } finally {
      setWiping(false);
    }
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <div className="flex items-center gap-1.5" style={{ color: 'var(--primary)' }}>
        <Sparkles size={20} />
        <h2 style={{ margin: 0 }}>{t('admin.seedTitle')}</h2>
      </div>
      <p className="text-muted">{t('admin.seedDesc')}</p>
      <button
        type="button"
        className="btn btn-primary flex items-center gap-1.5"
        onClick={handleSeedDemoData}
        disabled={seeding}
      >
        <Sparkles size={14} /> {seeding ? t('admin.seeding') : t('admin.seedTitle')}
      </button>
      {seedError && <p className="mt-16" style={{ color: 'var(--danger)' }}>{seedError}</p>}
      {seedResult && (
        <div className="card mt-16" style={{ padding: 16 }}>
          <h3 style={{ marginTop: 0, fontSize: 14 }}>{t('admin.seedCreatedTitle')}</h3>
          <table style={{ width: '100%', fontSize: 13 }}>
            <tbody>
              {Object.entries(seedResult).map(([table, count]) => (
                <tr key={table}>
                  <td style={{ padding: '3px 0' }}>{table}</td>
                  <td style={{ padding: '3px 0', textAlign: 'right' }}>{t('admin.rowsAdded', { count })}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="text-muted mt-8" style={{ fontSize: 12 }}>
            {t('admin.allDriversPasswordNote', { password: 'Surucu123!' })}
          </p>
        </div>
      )}

      <div className="flex items-center gap-1.5 mt-24" style={{ color: 'var(--danger)' }}>
        <AlertTriangle size={20} />
        <h2 style={{ margin: 0 }}>{t('admin.wipeTitle')}</h2>
      </div>
      <p className="text-muted">{t('admin.wipeDesc')}</p>
      <div className="card" style={{ padding: 16 }}>
        <label className="label">{t('admin.confirmDeleteLabel')}</label>
        <input
          className="input"
          value={wipeConfirmText}
          onChange={(e) => setWipeConfirmText(e.target.value)}
          placeholder={t('admin.confirmPlaceholder')}
          style={{ maxWidth: 200 }}
        />
        <button
          type="button"
          className="btn btn-danger mt-16 flex items-center gap-1.5"
          onClick={handleWipeDispatcherCustomer}
          disabled={!wipeCanSubmit || wiping}
        >
          <Trash2 size={14} /> {wiping ? t('admin.wiping') : t('admin.wipeBtn')}
        </button>
      </div>
      {wipeError && <p className="mt-16" style={{ color: 'var(--danger)' }}>{wipeError}</p>}
      {wipeResult && (
        <div className="card mt-16" style={{ padding: 16 }}>
          <h3 style={{ marginTop: 0, fontSize: 14 }}>{t('admin.wipeCompletedTitle')}</h3>
          <table style={{ width: '100%', fontSize: 13 }}>
            <tbody>
              {Object.entries(wipeResult).map(([table, count]) => (
                <tr key={table}>
                  <td style={{ padding: '3px 0' }}>{table}</td>
                  <td style={{ padding: '3px 0', textAlign: 'right' }}>{t('admin.rowsDeleted', { count })}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="flex items-center gap-1.5 mt-24" style={{ color: 'var(--danger)' }}>
        <AlertTriangle size={20} />
        <h2 style={{ margin: 0 }}>{t('admin.resetTitle')}</h2>
      </div>
      <p className="text-muted">{t('admin.resetDesc')}</p>
      <p style={{ color: 'var(--danger)', fontWeight: 600 }}>
        {t('admin.resetWarning')}
      </p>

      <div className="card" style={{ padding: 16 }}>
        <h3 style={{ marginTop: 0, fontSize: 14 }}>{t('admin.keptAccountsTitle')}</h3>
        {previewLoading && <p className="text-muted">{t('common.loading')}</p>}
        {!previewLoading && preview && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {preview.map((p) => (
              <div key={p.role}>
                <div className="flex items-center gap-1.5" style={{ fontSize: 13.5 }}>
                  <User size={14} style={{ color: 'var(--primary)' }} />
                  <span style={{ minWidth: 90, fontWeight: 600 }}>{ROLE_LABELS[p.role] || p.role}:</span>
                  {p.username ? (
                    <span>{p.fullName} — <span className="text-muted">{p.username}</span></span>
                  ) : (
                    <span className="text-muted">{t('admin.noAccountFound')}</span>
                  )}
                  {p.username && p.role !== 'ADMIN' && (
                    <button
                      type="button"
                      className="btn btn-sm flex items-center gap-1.5"
                      style={{ marginLeft: 'auto' }}
                      onClick={() => { setPwOpenRole(pwOpenRole === p.role ? null : p.role); setPwValue(''); }}
                    >
                      <KeyRound size={12} /> {t('admin.resetPasswordBtn')}
                    </button>
                  )}
                </div>

                {pwOpenRole === p.role && (
                  <div className="flex items-center gap-1.5 mt-8" style={{ marginLeft: 20 }}>
                    <input
                      type="text"
                      className="input"
                      placeholder={t('admin.newPasswordPlaceholder')}
                      value={pwValue}
                      onChange={(e) => setPwValue(e.target.value)}
                      style={{ maxWidth: 200 }}
                    />
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      onClick={() => handleResetPassword(p)}
                      disabled={!pwValue.trim() || pwSubmitting}
                    >
                      {pwSubmitting ? t('admin.updating') : t('common.save')}
                    </button>
                  </div>
                )}
                {pwMessage[p.role] === 'ok' && (
                  <p style={{ color: 'var(--success)', fontSize: 12, marginLeft: 20, marginTop: 4 }}>{t('admin.passwordUpdatedNote')}</p>
                )}
                {pwMessage[p.role] && pwMessage[p.role] !== 'ok' && (
                  <p style={{ color: 'var(--danger)', fontSize: 12, marginLeft: 20, marginTop: 4 }}>{pwMessage[p.role]}</p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card" style={{ padding: 16, marginTop: 16 }}>
        <label className="label">{t('admin.confirmDeleteLabel')}</label>
        <input
          className="input"
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          placeholder={t('admin.confirmPlaceholder')}
          style={{ maxWidth: 200 }}
        />
        <button
          type="button"
          className="btn btn-danger mt-16 flex items-center gap-1.5"
          onClick={handleReset}
          disabled={!canSubmit || submitting}
        >
          <Trash2 size={14} /> {submitting ? t('admin.wiping') : t('admin.resetBtn')}
        </button>
      </div>

      {error && <p className="mt-16" style={{ color: 'var(--danger)' }}>{error}</p>}

      {result && (
        <div className="card mt-16" style={{ padding: 16 }}>
          <h3 style={{ marginTop: 0 }}>{t('admin.wipeCompletedTitle')}</h3>
          <table style={{ width: '100%', fontSize: 13 }}>
            <tbody>
              {Object.entries(result).map(([table, count]) => (
                <tr key={table}>
                  <td style={{ padding: '3px 0' }}>{table}</td>
                  <td style={{ padding: '3px 0', textAlign: 'right' }}>{t('admin.rowsDeleted', { count })}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
