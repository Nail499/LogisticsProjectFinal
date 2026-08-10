// AI dəstək chat-i — üzən düymə (bütün müştəri/sürücü/dispetçer səhifələrində
// görünür, bax layouts/CustomerLayout.jsx, DriverLayout.jsx,
// DispatcherLayout.jsx). Backend Groq API-yə "tool use" (function calling) ilə
// müraciət edir ki, real sistem datasına (sifariş statusu, reyslər, flot
// vəziyyəti) əsaslanan cavab versin — bax SupportChatController,
// service/AiChatService, service/AiToolExecutor. Söhbət tarixçəsi yalnız bu
// komponentin React state-ində saxlanılır (səhifə yenilənəndə itir) — hər
// sorğuda bütün tarixçə backend-ə göndərilir, backend özü heç nə yaddaşda
// saxlamır (bax AiChatService qeydi).
//
// Rəng qeydi: bu komponent DashboardLayout-dan kənarda, ona bacı (sibling)
// kimi render olunur (bax yuxarıdakı 3 layout faylı) — səbəb position:fixed
// üçün containing-block problemi (bax TripDetailModal.jsx-də ətraflı izah).
// Amma bunun nəticəsi kimi .theme-orange class-ının DOM alt-ağacına DAXİL
// DEYİL, ona görə var(--primary) burada Fleetra narıncısı yox, index.css-in
// defolt (mavi) --primary dəyərini verirdi. Ona görə narıncı rənglər burada
// birbaşa (hardcode) yazılır, CSS dəyişəni ilə deyil.
const BRAND = '#fe8704';
const BRAND_DARK = '#e07600';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Bot, X, Send, Sparkles } from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function SupportChatWidget() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (open) bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, open]);

  const send = async (e) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed || sending) return;
    const updated = [...messages, { role: 'user', content: trimmed }];
    setMessages(updated);
    setText('');
    setSending(true);
    try {
      const res = await axiosClient.post('/api/support-chat', { messages: updated });
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.reply }]);
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', content: t('supportChat.error') }]);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        style={{
          position: 'fixed',
          bottom: 20,
          right: 20,
          zIndex: 1000,
          width: 52,
          height: 52,
          borderRadius: '50%',
          border: 'none',
          background: BRAND,
          color: '#fff',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 14px rgba(0,0,0,0.25)',
        }}
        title={t('supportChat.launcherTitle')}
      >
        {open ? <X size={22} /> : <Bot size={24} />}
      </button>

      {open && (
        <div
          style={{
            position: 'fixed',
            bottom: 84,
            right: 20,
            zIndex: 1000,
            width: 340,
            maxWidth: 'calc(100vw - 40px)',
            height: 440,
            maxHeight: 'calc(100vh - 110px)',
            display: 'flex',
            flexDirection: 'column',
            border: '1px solid var(--border)',
            borderRadius: 14,
            overflow: 'hidden',
            background: 'var(--surface)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
          }}
        >
          <div style={{ padding: '10px 14px', background: BRAND, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sparkles size={16} />
            <div style={{ fontSize: 13.5, fontWeight: 700 }}>{t('supportChat.title')}</div>
          </div>

          <div style={{ flex: 1, overflowY: 'auto', padding: 10, background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: 6 }}>
            {messages.length === 0 && (
              <div className="text-muted" style={{ textAlign: 'center', fontSize: 12.5, marginTop: 20 }}>
                {t('supportChat.emptyState')}
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: m.role === 'user' ? 'flex-end' : 'flex-start' }}>
                <div
                  style={{
                    maxWidth: '82%',
                    padding: '7px 11px',
                    borderRadius: 12,
                    fontSize: 13,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    background: m.role === 'user' ? BRAND : 'var(--surface)',
                    color: m.role === 'user' ? '#fff' : 'var(--text)',
                    border: m.role === 'user' ? 'none' : '1px solid var(--border)',
                  }}
                >
                  {m.content}
                </div>
              </div>
            ))}
            {sending && (
              <div className="text-muted" style={{ fontSize: 12, alignSelf: 'flex-start' }}>{t('supportChat.typing')}</div>
            )}
            <div ref={bottomRef} />
          </div>

          <form onSubmit={send} style={{ display: 'flex', gap: 6, padding: 8, borderTop: '1px solid var(--border)', background: 'var(--surface)' }}>
            <input
              className="input"
              style={{ flex: 1, fontSize: 13, padding: '7px 10px' }}
              placeholder={t('supportChat.placeholder')}
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={1000}
            />
            <button
              type="submit"
              className="btn btn-sm"
              disabled={!text.trim() || sending}
              style={{ padding: '7px 10px', background: BRAND, borderColor: BRAND, color: '#fff' }}
              onMouseEnter={(e) => { e.currentTarget.style.background = BRAND_DARK; e.currentTarget.style.borderColor = BRAND_DARK; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = BRAND; e.currentTarget.style.borderColor = BRAND; }}
            >
              <Send size={14} />
            </button>
          </form>
        </div>
      )}
    </>
  );
}
