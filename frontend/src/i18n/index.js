// Central i18next configuration.
// 3 languages: AZ / RU / EN. Default is EN when the visitor has no saved
// preference yet (see LANG_STORAGE_KEY below) — the app previously had an
// inconsistent mix (landing/public pages in English, authenticated panels
// in Azerbaijani); this restores a single switchable source of truth.
// Selection is persisted in localStorage so it survives reloads/relogins,
// independent of the browser's own locale (we intentionally do NOT use
// i18next-browser-languagedetector's navigator-language detection, since
// the user asked for a fixed EN default rather than guessing from the
// browser).
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './locales/en.json';
import az from './locales/az.json';
import ru from './locales/ru.json';

export const LANG_STORAGE_KEY = 'fleetra_lang';

export const SUPPORTED_LANGUAGES = [
  { code: 'en', label: 'EN', name: 'English' },
  { code: 'az', label: 'AZ', name: 'Azərbaycanca' },
  { code: 'ru', label: 'RU', name: 'Русский' },
];

const savedLang = typeof window !== 'undefined' ? window.localStorage.getItem(LANG_STORAGE_KEY) : null;
const initialLang = SUPPORTED_LANGUAGES.some((l) => l.code === savedLang) ? savedLang : 'en';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      az: { translation: az },
      ru: { translation: ru },
    },
    lng: initialLang,
    fallbackLng: 'en',
    supportedLngs: ['en', 'az', 'ru'],
    interpolation: { escapeValue: false },
  });

// Keep localStorage in sync whenever the language changes (e.g. via
// LanguageSwitcher), regardless of which component triggered the change.
i18n.on('languageChanged', (lng) => {
  if (typeof window !== 'undefined') window.localStorage.setItem(LANG_STORAGE_KEY, lng);
});

export default i18n;
