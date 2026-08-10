import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../context/AuthContext.jsx';
import Reveal from '../../components/Reveal.jsx';
import VehiclePhotosCard from '../../components/driver/VehiclePhotosCard.jsx';
import { User, Camera, Save, KeyRound, Mail, Send, ShieldCheck } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function calcAge(dateOfBirth) {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const m = today.getMonth() - dob.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) age--;
  return age;
}

export default function ProfilePage() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const fileInputRef = useRef(null);

  const ROLE_LABELS = {
    ADMIN: t('profile.roleAdmin'),
    DISPATCHER: t('profile.roleDispatcher'),
    DRIVER: t('profile.roleDriver'),
    CUSTOMER: t('profile.roleCustomer'),
  };

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [savingCreds, setSavingCreds] = useState(false);
  const [notice, setNotice] = useState(null); // { type: 'success'|'error', text }

  const [form, setForm] = useState({ fullName: '', dateOfBirth: '', nationality: '', location: '' });
  const [creds, setCreds] = useState({ currentPassword: '', newUsername: '', newPassword: '' });

  // Email dəyişmə axını: 'idle' (sadəcə göstərir) -> 'enter-new' (yeni email
  // xanası) -> 'enter-code' (email-ə gələn kodu təsdiqlə). Bax backend
  // ProfileController#requestEmailChange/confirmEmailChange — köhnə email
  // yalnız DOĞRU kod daxil ediləndə dəyişir.
  const [emailStep, setEmailStep] = useState('idle');
  const [newEmailInput, setNewEmailInput] = useState('');
  const [emailCode, setEmailCode] = useState('');
  const [emailSaving, setEmailSaving] = useState(false);
  const [emailNotice, setEmailNotice] = useState('');

  const hasExtraFields = user?.role === 'CUSTOMER' || user?.role === 'DRIVER';
  const canChangeEmail = user?.role !== 'ADMIN';

  useEffect(() => {
    axiosClient.get('/api/profile')
      .then((res) => {
        setProfile(res.data);
        setForm({
          fullName: res.data.fullName || '',
          dateOfBirth: res.data.dateOfBirth || '',
          nationality: res.data.nationality || '',
          location: res.data.location || '',
        });
        setCreds((c) => ({ ...c, newUsername: res.data.username || '' }));
      })
      .finally(() => setLoading(false));
  }, []);

  const showNotice = (type, text) => {
    setNotice({ type, text });
    setTimeout(() => setNotice(null), 4000);
  };

  const handleFormChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const handleCredsChange = (e) => setCreds({ ...creds, [e.target.name]: e.target.value });

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await axiosClient.put('/api/profile', form);
      setProfile(res.data);
      showNotice('success', t('profile.noticeProfileUpdated'));
    } catch (err) {
      showNotice('error', err.response?.data?.message || t('profile.errProfileUpdate'));
    } finally {
      setSaving(false);
    }
  };

  const handleSaveCredentials = async (e) => {
    e.preventDefault();
    if (!creds.currentPassword) {
      showNotice('error', t('profile.errCurrentPasswordRequired'));
      return;
    }
    setSavingCreds(true);
    try {
      const usernameChanged = creds.newUsername && creds.newUsername !== profile.username;
      await axiosClient.put('/api/profile/credentials', {
        currentPassword: creds.currentPassword,
        newUsername: creds.newUsername || undefined,
        newPassword: creds.newPassword || undefined,
      });
      showNotice('success', t('profile.noticeCredsUpdated'));
      setCreds({ currentPassword: '', newUsername: creds.newUsername, newPassword: '' });
      if (usernameChanged) {
        showNotice('success', t('profile.noticeUsernameChanged'));
        setTimeout(() => logout(), 1500);
      }
    } catch (err) {
      showNotice('error', err.response?.data?.message || t('profile.errCredsUpdate'));
    } finally {
      setSavingCreds(false);
    }
  };

  const handleRequestEmailCode = async (e) => {
    e.preventDefault();
    if (!newEmailInput.trim()) return;
    setEmailSaving(true);
    setEmailNotice('');
    try {
      await axiosClient.post('/api/profile/email/request-change', { newEmail: newEmailInput.trim() });
      setEmailNotice('');
      setEmailStep('enter-code');
    } catch (err) {
      setEmailNotice(err.response?.data?.message || t('profile.errCodeSend'));
    } finally {
      setEmailSaving(false);
    }
  };

  const handleConfirmEmailCode = async (e) => {
    e.preventDefault();
    if (!emailCode.trim()) return;
    setEmailSaving(true);
    setEmailNotice('');
    try {
      const res = await axiosClient.post('/api/profile/email/confirm-change', { code: emailCode.trim() });
      setProfile(res.data);
      setEmailStep('idle');
      setNewEmailInput('');
      setEmailCode('');
      showNotice('success', t('profile.noticeEmailChanged'));
    } catch (err) {
      setEmailNotice(err.response?.data?.message || t('profile.errCodeInvalid'));
    } finally {
      setEmailSaving(false);
    }
  };

  const cancelEmailChange = () => {
    setEmailStep('idle');
    setNewEmailInput('');
    setEmailCode('');
    setEmailNotice('');
  };

  const handlePhotoPick = () => fileInputRef.current?.click();

  const handlePhotoChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('photo', file);
      // Content-Type QƏSDƏN "undefined" edilir, "multipart/form-data" YOX:
      // axiosClient instansiyası defolt olaraq Content-Type: application/json
      // qoyur, biz onu FormData göndərəndə override etməliyik — amma əl ilə
      // "multipart/form-data" yazsaq, brauzer bu explicit header-i artıq özü
      // dəyişmir və boundary əlavə etmir (boundary=... hissəsi olmadan
      // multipart body backend-də parse oluna bilmir → 400/500 xəta). undefined
      // versək, axios bu açarı silir və brauzer FormData göndərəndə düzgün
      // "multipart/form-data; boundary=..." header-ini özü yaradır.
      const res = await axiosClient.post('/api/profile/photo', fd, {
        headers: { 'Content-Type': undefined },
      });
      setProfile(res.data);
      showNotice('success', t('profile.noticePhotoUploaded'));
    } catch (err) {
      showNotice('error', t('profile.errPhotoUpload'));
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  if (loading) return <p>{t('common.loading')}</p>;

  const age = calcAge(form.dateOfBirth);

  return (
    <div>
      <Reveal>
        <h2>{t('profile.title')}</h2>
        <p className="text-muted">{t('profile.subtitle')}</p>
      </Reveal>

      {notice && (
        <div className={`alert ${notice.type === 'error' ? 'alert-error' : 'alert-success'} mt-16`}>
          {notice.text}
        </div>
      )}

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        {/* Şəxsi məlumatlar */}
        <Reveal delay={60}>
          <div className="card hover-lift">
            <h3>{t('profile.personalInfoTitle')}</h3>

            {hasExtraFields && (
              <div className="flex-between" style={{ marginBottom: 18 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <div
                    style={{
                      width: 64, height: 64, borderRadius: '50%', overflow: 'hidden',
                      background: '#fff5ea', display: 'flex', alignItems: 'center', justifyContent: 'center',
                      border: '2px solid #fe8704', flexShrink: 0,
                    }}
                  >
                    {profile?.photoUrl ? (
                      <img src={API_BASE + profile.photoUrl} alt={t('profile.photoAlt')} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : (
                      <User size={28} color="#fe8704" />
                    )}
                  </div>
                  <div>
                    <button type="button" className="btn btn-sm" onClick={handlePhotoPick} disabled={uploading}>
                      <Camera size={14} /> {uploading ? t('common.loading') : t('profile.changePhotoBtn')}
                    </button>
                    <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handlePhotoChange} />
                  </div>
                </div>
              </div>
            )}

            <form onSubmit={handleSaveProfile}>
              <div className="form-group">
                <label className="label">{t('admin.fullNameLabel')}</label>
                <input className="input" name="fullName" value={form.fullName} onChange={handleFormChange} required />
              </div>

              {hasExtraFields && (
                <>
                  <div className="grid grid-2">
                    <div className="form-group">
                      <label className="label">{t('profile.dobLabel')} {age !== null && t('profile.ageSuffix', { age })}</label>
                      <input type="date" className="input" name="dateOfBirth" value={form.dateOfBirth || ''} onChange={handleFormChange} />
                    </div>
                    <div className="form-group">
                      <label className="label">{t('profile.nationalityLabel')}</label>
                      <input className="input" name="nationality" value={form.nationality} onChange={handleFormChange} placeholder={t('profile.nationalityPlaceholder')} />
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="label">{t('profile.locationLabel')}</label>
                    <input className="input" name="location" value={form.location} onChange={handleFormChange} placeholder={t('profile.locationPlaceholder')} />
                  </div>
                </>
              )}

              <button type="submit" className="btn btn-primary mt-16" disabled={saving}>
                <Save size={15} /> {saving ? t('profile.saving') : t('common.save')}
              </button>
            </form>
          </div>
        </Reveal>

        {/* Giriş məlumatları */}
        <Reveal delay={120}>
          <div className="card hover-lift">
            <h3>{t('profile.loginInfoTitle')}</h3>
            <p className="text-muted" style={{ marginTop: -8, marginBottom: 16 }}>
              {t('profile.accountLabel', { role: ROLE_LABELS[user?.role] || user?.role, username: profile?.username })}
            </p>
            <form onSubmit={handleSaveCredentials}>
              <div className="form-group">
                <label className="label">{t('profile.usernameLabel')}</label>
                <input className="input" name="newUsername" value={creds.newUsername} onChange={handleCredsChange} required />
              </div>
              <div className="form-group">
                <label className="label">{t('profile.newPasswordLabel')}</label>
                <input type="password" className="input" name="newPassword" value={creds.newPassword} onChange={handleCredsChange} placeholder="••••••••" />
              </div>
              <div className="form-group">
                <label className="label">{t('profile.currentPasswordLabel')}</label>
                <input type="password" className="input" name="currentPassword" value={creds.currentPassword} onChange={handleCredsChange} required placeholder="••••••••" />
              </div>

              <button type="submit" className="btn btn-primary mt-16" disabled={savingCreds}>
                <KeyRound size={15} /> {savingCreds ? t('profile.saving') : t('profile.updateCredsBtn')}
              </button>
            </form>
          </div>
        </Reveal>
      </div>

      {/* Email — ADMIN xaric bütün rollar. Kodla dəyişmə axını: yeni email
          daxil et -> kod göndərilir -> kodu təsdiqlə -> email dəyişir. */}
      {canChangeEmail && (
        <Reveal delay={150}>
          <div className="card hover-lift mt-16" style={{ maxWidth: 480 }}>
            <div className="flex items-center gap-1.5">
              <Mail size={16} style={{ color: 'var(--primary)' }} />
              <h3 style={{ margin: 0 }}>{t('profile.emailTitle')}</h3>
            </div>

            {emailStep === 'idle' && (
              <div className="flex-between mt-8">
                <span>{profile?.email || <span className="text-muted">{t('profile.emailNotSet')}</span>}</span>
                <button type="button" className="btn btn-sm" onClick={() => setEmailStep('enter-new')}>
                  {t('profile.changeBtn')}
                </button>
              </div>
            )}

            {emailStep === 'enter-new' && (
              <form onSubmit={handleRequestEmailCode} className="mt-8">
                <div className="form-group">
                  <label className="label">{t('profile.newEmailLabel')}</label>
                  <input
                    type="email"
                    className="input"
                    value={newEmailInput}
                    onChange={(e) => setNewEmailInput(e.target.value)}
                    placeholder={t('profile.newEmailPlaceholder')}
                    required
                  />
                </div>
                {emailNotice && <p style={{ fontSize: 12.5, color: 'var(--danger)' }}>{emailNotice}</p>}
                <div className="flex items-center gap-1.5">
                  <button type="submit" className="btn btn-primary btn-sm flex items-center gap-1.5" disabled={emailSaving}>
                    <Send size={13} /> {emailSaving ? t('profile.sending') : t('profile.sendCodeBtn')}
                  </button>
                  <button type="button" className="btn btn-sm" onClick={cancelEmailChange}>{t('common.cancel')}</button>
                </div>
              </form>
            )}

            {emailStep === 'enter-code' && (
              <form onSubmit={handleConfirmEmailCode} className="mt-8">
                <p className="text-muted" style={{ fontSize: 12.5, marginTop: -4 }}>
                  {t('profile.codeSentHint', { email: newEmailInput })}
                </p>
                <div className="form-group">
                  <label className="label">{t('profile.confirmCodeLabel')}</label>
                  <input
                    className="input"
                    value={emailCode}
                    onChange={(e) => setEmailCode(e.target.value)}
                    placeholder="000000"
                    maxLength={6}
                    required
                  />
                </div>
                {emailNotice && <p style={{ fontSize: 12.5, color: 'var(--danger)' }}>{emailNotice}</p>}
                <div className="flex items-center gap-1.5">
                  <button type="submit" className="btn btn-primary btn-sm flex items-center gap-1.5" disabled={emailSaving}>
                    <ShieldCheck size={13} /> {emailSaving ? t('profile.verifying') : t('profile.confirmBtn')}
                  </button>
                  <button type="button" className="btn btn-sm" onClick={cancelEmailChange}>{t('common.cancel')}</button>
                </div>
              </form>
            )}
          </div>
        </Reveal>
      )}

      {/* Nəqliyyat vasitəm — yalnız sürücü. Öz maşınının 1 əsas + ən çoxu 4
          ətraflı şəklini yükləyir; müştəri bunları tracking səhifəsində
          görür (bax LiveTrackingPanel.jsx). */}
      {user?.role === 'DRIVER' && (
        <Reveal delay={180}>
          <div className="mt-16">
            <VehiclePhotosCard />
          </div>
        </Reveal>
      )}
    </div>
  );
}
