import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { UserPlus, User, Phone, Mail, Building2, Lock, ArrowLeft, CheckCircle2, ShieldCheck } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Reveal from '../../components/Reveal.jsx';
import PasswordStrength from '../../components/PasswordStrength.jsx';
import LanguageSwitcher from '../../components/LanguageSwitcher.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { isPasswordStrong } from '../../utils/passwordRules.js';

import bannerHero from '../../assets/logzee/banner_slider_4.jpg';
import logoFooter from '../../assets/fleetra-mark.svg';

const FIELD_META = [
  { name: 'fullName', labelKey: 'auth.fullNameLabel', icon: User, type: 'text', required: true },
  { name: 'phone', labelKey: 'auth.phoneLabel', icon: Phone, type: 'text', required: true },
  { name: 'email', labelKey: 'auth.emailLabel', icon: Mail, type: 'email', required: true },
  { name: 'companyName', labelKey: 'auth.companyNameLabel', icon: Building2, type: 'text', required: false },
  { name: 'username', labelKey: 'auth.usernameFieldLabel', icon: User, type: 'text', required: true },
  { name: 'password', labelKey: 'auth.passwordFieldLabel', icon: Lock, type: 'password', required: true },
];

export default function CustomerRegister() {
  const { t } = useTranslation();
  const FIELDS = FIELD_META.map((f) => ({ ...f, label: t(f.labelKey) }));
  const navigate = useNavigate();
  const { setSession } = useAuth();
  const [form, setForm] = useState({
    username: '', password: '', fullName: '', phone: '', email: '', companyName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // 'form' -> qeydiyyat formu, 'verify' -> email-ə göndərilən kodun daxil
  // edilməsi addımı. Qeydiyyat uğurlu olduqda "verify"-ə keçirik, sonra
  // kod düzgün olduqda backend token qaytarır və birbaşa login edilir.
  const [step, setStep] = useState('form');
  const [code, setCode] = useState('');
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [resendMsg, setResendMsg] = useState('');

  // Şifrə tələbləri qutusu yalnız istifadəçi şifrə sahəsinə fokuslananda
  // açılır, fokusdan çıxanda gizlənir (bax PasswordStrength.jsx).
  const [passwordFocused, setPasswordFocused] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await axiosClient.post('/api/auth/register/customer', form);
      setStep('verify');
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || t('auth.registerError'));
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setVerifyLoading(true);
    try {
      const res = await axiosClient.post('/api/auth/verify-email', {
        username: form.username,
        code,
      });
      setSession(res.data);
      navigate('/customer');
    } catch (err) {
      setError(err.response?.data?.message || t('auth.verifyError'));
    } finally {
      setVerifyLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setResendMsg('');
    try {
      await axiosClient.post('/api/auth/resend-verification', { username: form.username });
      setResendMsg(t('auth.resendSuccess'));
    } catch (err) {
      setError(err.response?.data?.message || t('auth.resendError'));
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-12"
      style={{ fontFamily: "'Poppins', sans-serif" }}
    >
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: `url(${bannerHero})` }}
      />
      <div className="absolute inset-0 bg-black/72" />

      <div className="absolute right-4 top-4 z-[60] sm:right-8 sm:top-8">
        <LanguageSwitcher variant="light" />
      </div>

      <Reveal className="relative w-full max-w-lg">
        <div className="rounded-2xl bg-white p-8 shadow-2xl sm:p-10">
          <div className="flex items-center justify-center gap-2.5">
            <img src={logoFooter} alt="Fleetra" className="h-9 w-auto" />
            <span className="text-xl font-extrabold" style={{ color: '#111827' }}>
              Fleet<span style={{ color: '#fe8704' }}>ra</span>
            </span>
          </div>

          {step === 'form' && (
            <>
              <h2
                className="text-center font-extrabold"
                style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}
              >
                {t('auth.registerTitle')}
              </h2>
              <p className="text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
                {t('auth.registerSubtitle')}
              </p>

              {error && (
                <div
                  className="mt-5 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
                >
                  {String(error)}
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
                {FIELDS.map((f) => (
                  <div key={f.name} style={f.name === 'password' ? { position: 'relative' } : undefined}>
                    <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                      {f.label}
                    </label>
                    <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                      <f.icon size={17} style={{ color: '#fe8704' }} />
                      <input
                        type={f.type}
                        name={f.name}
                        className="w-full border-none bg-transparent p-0 text-sm outline-none"
                        style={{ color: '#111827' }}
                        value={form[f.name]}
                        onChange={handleChange}
                        onFocus={f.name === 'password' ? () => setPasswordFocused(true) : undefined}
                        onBlur={f.name === 'password' ? () => setPasswordFocused(false) : undefined}
                        required={f.required}
                      />
                    </div>
                    {f.name === 'password' && <PasswordStrength password={form.password} show={passwordFocused} />}
                  </div>
                ))}

                <button
                  type="submit"
                  disabled={loading || !isPasswordStrong(form.password)}
                  className="mt-2 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5 disabled:opacity-60"
                  style={{ background: '#fe8704', color: '#ffffff' }}
                >
                  {loading ? t('auth.registerSubmitting') : (<>{t('auth.registerSubmit')} <UserPlus size={16} /></>)}
                </button>
              </form>
            </>
          )}

          {step === 'verify' && (
            <>
              <h2
                className="text-center font-extrabold"
                style={{ color: '#111827', fontSize: '1.75rem', margin: '28px 0 0' }}
              >
                {t('auth.verifyTitle')}
              </h2>
              <p className="text-center text-sm" style={{ color: '#6b7280', margin: '8px 0 0' }}>
                {t('auth.verifySubtitle')} — <strong>{form.email}</strong>
              </p>

              {error && (
                <div
                  className="mt-5 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca' }}
                >
                  {String(error)}
                </div>
              )}
              {resendMsg && (
                <div
                  className="mt-5 flex items-center gap-2 rounded-lg px-4 py-3 text-sm"
                  style={{ background: '#f0fdf4', color: '#16a34a', border: '1px solid #bbf7d0' }}
                >
                  <CheckCircle2 size={16} /> {resendMsg}
                </div>
              )}

              <form onSubmit={handleVerify} className="mt-7 flex flex-col gap-4">
                <div>
                  <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide" style={{ color: '#374151' }}>
                    {t('auth.verifyCodeLabel')}
                  </label>
                  <div className="flex items-center gap-2.5 rounded-lg border px-3.5 py-3" style={{ borderColor: '#e5e7eb' }}>
                    <ShieldCheck size={17} style={{ color: '#fe8704' }} />
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      className="w-full border-none bg-transparent p-0 text-center text-lg tracking-[0.5em] outline-none"
                      style={{ color: '#111827' }}
                      value={code}
                      onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                      required
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={verifyLoading || code.length !== 6}
                  className="mt-2 flex items-center justify-center gap-2 rounded-lg py-3.5 text-sm font-bold transition-transform hover:-translate-y-0.5 disabled:opacity-60"
                  style={{ background: '#fe8704', color: '#ffffff' }}
                >
                  {verifyLoading ? t('auth.verifying') : (<>{t('auth.verifyButton')} <ShieldCheck size={16} /></>)}
                </button>

                <button
                  type="button"
                  onClick={handleResend}
                  className="text-center text-[13px] font-semibold"
                  style={{ color: '#fe8704' }}
                >
                  {t('auth.resendCode')}
                </button>
              </form>
            </>
          )}

          <p className="mt-6 text-center text-[13px]" style={{ color: '#6b7280', margin: '24px 0 0' }}>
            {t('auth.alreadyHaveAccount')}{' '}
            <Link to="/login" className="font-semibold" style={{ color: '#fe8704' }}>
              {t('auth.signIn')}
            </Link>
          </p>

          <Link
            to="/"
            className="mt-5 flex items-center justify-center gap-1.5 text-[13px] font-semibold"
            style={{ color: '#111827', marginTop: '20px' }}
          >
            <ArrowLeft size={14} /> {t('auth.backToHome')}
          </Link>
        </div>
      </Reveal>
    </div>
  );
}
