import { Check, X } from 'lucide-react';
import { PASSWORD_RULES } from '../utils/passwordRules.js';

// Şifrə sahəsi fokuslananda altında açılan üzən qutu (popover) — tələblər
// yalnız istifadəçi sahəyə klikləyib yazmağa başlayanda görünür, fokus
// itəndə (klikdən çıxanda) yenidən gizlənir. `show` valideynin
// onFocus/onBlur ilə idarə etdiyi fokus vəziyyətindən gəlir (bax
// CustomerRegister.jsx/ForgotPassword.jsx). Parent input-un wrapper div-i
// `position: relative` olmalıdır ki, bu qutu düzgün yerdə "asılsın".
export default function PasswordStrength({ password, show }) {
  if (!show) return null;
  const touched = (password || '').length > 0;

  return (
    <div
      style={{
        position: 'absolute', top: 'calc(100% + 6px)', left: 0, right: 0, zIndex: 20,
        background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: 10,
        boxShadow: '0 8px 24px rgba(15,23,42,0.12)', padding: '10px 14px',
      }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
        {PASSWORD_RULES.map((rule) => {
          const ok = rule.test(password || '');
          const color = !touched ? '#9ca3af' : ok ? '#16a34a' : '#dc2626';
          return (
            <div key={rule.key} className="flex items-center gap-1.5" style={{ fontSize: 12, color }}>
              {touched ? (ok ? <Check size={12} /> : <X size={12} />) : (
                <span style={{ width: 12, height: 12, borderRadius: '50%', border: '1.5px solid #d1d5db', display: 'inline-block' }} />
              )}
              {rule.label}
            </div>
          );
        })}
      </div>
    </div>
  );
}
