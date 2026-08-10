import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Hər sorğuya avtomatik JWT token əlavə et. sessionStorage istifadə olunur
// (localStorage YOX) — bax AuthContext.jsx-dəki qeyd: localStorage bütün
// tab-lar arasında paylaşıldığı üçün fərqli rollarla paralel tab-larda test
// edəndə bir tab-ın giriş/çıxışı digərlərinin tokenini səssizcə pozurdu və
// "token tez bitir" kimi hiss olunan sporadik 401-logout-lara səbəb olurdu.
axiosClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('lms_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token etibarsızdırsa (401), avtomatik logout et
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('lms_token');
      sessionStorage.removeItem('lms_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
