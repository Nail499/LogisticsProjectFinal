// Şifrə tələbləri — həm qeydiyyat (CustomerRegister.jsx), həm də şifrə
// bərpası (ForgotPassword.jsx) eyni siyahını istifadə edir ki, tələblər
// hər iki yerdə eyni görünsün. Backend (AuthController#isStrongPassword)
// eyni qaydanı təkrarlayır — frontend-dəki yoxlama yalnız UX üçündür,
// əsl mühafizə backend-dədir. Etiketlər ingiliscədir — CustomerRegister.jsx
// və Login.jsx da (bu şifrə sahələrinin olduğu yeganə səhifələr) qəsdən
// ingiliscədir, tətbiqin qalanından fərqli olaraq.
export const PASSWORD_RULES = [
  { key: 'length', label: 'At least 8 characters', test: (pw) => pw.length >= 8 },
  { key: 'upper', label: 'At least 1 uppercase letter (A-Z)', test: (pw) => /[A-Z]/.test(pw) },
  { key: 'lower', label: 'At least 1 lowercase letter (a-z)', test: (pw) => /[a-z]/.test(pw) },
  { key: 'digit', label: 'At least 1 number (0-9)', test: (pw) => /[0-9]/.test(pw) },
];

export function isPasswordStrong(password) {
  return PASSWORD_RULES.every((rule) => rule.test(password || ''));
}
