import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Sun, Moon } from 'lucide-react';
import { getStoredTheme, applyTheme } from '../utils/theme.js';

// Tünd/işıqlı rejim keçid düyməsi — DashboardLayout topbar-ında bütün
// rollar (müştəri/sürücü/dispetçer/admin) üçün ortaq göstərilir. Faktiki
// rəng dəyişikliyi CSS dəyişənləri ilə olur (bax index.css
// :root[data-theme="dark"]), bu komponent sadəcə atributu keçirir.
export default function ThemeToggle() {
  const { t } = useTranslation();
  const [theme, setTheme] = useState(getStoredTheme);

  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    setTheme(next);
  };

  return (
    <button
      type="button"
      onClick={toggle}
      className="btn btn-sm"
      style={{ padding: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
      title={theme === 'dark' ? t('common.switchToLight') : t('common.switchToDark')}
      aria-label={t('common.switchThemeAria')}
    >
      {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
    </button>
  );
}
