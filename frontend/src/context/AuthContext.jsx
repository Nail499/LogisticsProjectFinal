import { createContext, useContext, useState } from 'react';
import axiosClient from '../api/axiosClient';

const AuthContext = createContext(null);

// Qeyd: qəsdən sessionStorage istifadə olunur, localStorage YOX. localStorage
// eyni brauzerin BÜTÜN tab-ları arasında paylaşılır — bu tətbiqdə tez-tez
// eyni anda fərqli rollarla (sürücü, dispetçer, müştəri, admin) fərqli
// tab-larda test edildiyi üçün, bir tab-da giriş/çıxış digər tab-ların
// tokenini səssizcə əvəz edir/silirdi: A tab-ı sürücü kimi açıq qalır, B
// tab-ında admin kimi çıxış edilir -> A tab-ının növbəti sorğusu artıq
// tokensiz gedir -> backend 401 qaytarır -> axiosClient A tab-ını da
// məcburi çıxış edir. Nəticə: "token tez bitir" kimi hiss olunan, əslində
// tamam başqa bir tab-ın çıxışından qaynaqlanan sporadik logout-lar.
// sessionStorage hər tab üçün ayrıdır, bu problemi kökündən aradan qaldırır
// (əvəzində: tab bağlananda həmin tab-ın sessiyası bitir — bu tətbiqin çoxrollu
// test rejimi üçün daha doğru davranışdır).
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('lms_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (username, password) => {
    const res = await axiosClient.post('/api/auth/login', { username, password });
    const { token, role, username: uname } = res.data;
    sessionStorage.setItem('lms_token', token);
    const userData = { username: uname, role };
    sessionStorage.setItem('lms_user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = () => {
    sessionStorage.removeItem('lms_token');
    sessionStorage.removeItem('lms_user');
    setUser(null);
  };

  // /api/auth/verify-email və /api/auth/reset-password kimi endpoint-lər
  // (login deyil) birbaşa { token, role, username } formasında cavab qaytarır
  // — bu, login() sorğusunu təkrar etmədən eyni sessiyanı quraşdırır.
  const setSession = ({ token, role, username }) => {
    sessionStorage.setItem('lms_token', token);
    const userData = { username, role };
    sessionStorage.setItem('lms_user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, setSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
