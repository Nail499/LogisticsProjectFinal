const STORAGE_KEY = 'fleetra_theme';

// Dark mode saxlama/tətbiq helper-i (bax index.html-dəki inline script —
// React yüklənməzdən əvvəl eyni açardan oxuyub atributu qoyur ki, səhifə
// açılışında qısa "işıqlı flash" görünməsin).
export function getStoredTheme() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

export function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // localStorage bloklanıbsa (məs. private rejim) — tema sadəcə bu
    // sessiyada tətbiq olunur, yenidən açılanda default-a qayıdır.
  }
}
