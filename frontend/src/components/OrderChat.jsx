import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Send, MessageCircle, Image as ImageIcon } from 'lucide-react';
import axiosClient from '../api/axiosClient';
import { subscribeTopic } from '../utils/socket.js';
import PhotoLightbox from './PhotoLightbox.jsx';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function formatTime(iso, locale) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
}

// Sifariş üzrə canlı yazışma paneli (bax ChatController/ChatService) —
// eyni cargoId altında ÜÇ otaqdan biri: CUSTOMER_DRIVER (müştəri <-> sürücü),
// CUSTOMER_DISPATCHER (müştəri <-> dispetçer/admin) və ya INTERNAL (yalnız
// sürücü + dispetçer/admin, müştəri bura girə bilmir — bax entity/ChatChannel).
// "channel" prop-u MƏCBURİDİR, çağıran tərəf həmişə açıq göstərməlidir —
// backend-də də default yoxdur (bax ChatController). Tarixçə REST ilə
// yüklənir, yeni mesajlar isə mövcud STOMP kanalı üzərindən (bax
// utils/socket.js — eyni infrastruktur canlı GPS izləməsi üçün istifadə
// olunur) real-vaxt gəlir, səhifəni yeniləmək lazım deyil. Mətnlə yanaşı
// şəkil də göndərilə bilər (bax handleImagePick).
export default function OrderChat({ cargoId, channel }) {
  const { t, i18n } = useTranslation();
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [error, setError] = useState('');
  const [lightboxUrl, setLightboxUrl] = useState(null);
  const bottomRef = useRef(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (!cargoId) return;
    setLoading(true);
    axiosClient.get(`/api/chat/cargo/${cargoId}/messages`, { params: { channel } })
      .then((res) => setMessages(res.data))
      .catch(() => setError(t('chat.errLoad')))
      .finally(() => setLoading(false));

    // Hər 3 kanal üçün AYRI STOMP mövzuları (bax ChatService#topicSuffix) —
    // belə məs. müştərinin brauzeri INTERNAL otağın yayımına heç vaxt abunə
    // olmur.
    const topicSuffix = channel === 'CUSTOMER_DRIVER' ? '/driver' : channel === 'INTERNAL' ? '/internal' : '/dispatcher';
    const topic = `/topic/chat/${cargoId}${topicSuffix}`;
    const unsubscribe = subscribeTopic(topic, (incoming) => {
      setMessages((prev) => [...prev, incoming]);
    });
    return unsubscribe;
  }, [cargoId, channel]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!text.trim() || sending) return;
    setSending(true);
    setError('');
    try {
      await axiosClient.post(`/api/chat/cargo/${cargoId}/messages`, { message: text.trim() }, { params: { channel } });
      setText('');
      // Öz mesajımızı burada əlavə etmirik — STOMP broadcast bunu geri
      // qaytaracaq (bax ChatService#sendMessage), belə ikiqat görünmə olmur.
    } catch (err) {
      setError(err.response?.data?.message || t('chat.errSend'));
    } finally {
      setSending(false);
    }
  };

  const handleImagePick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setUploadingImage(true);
    setError('');
    try {
      const fd = new FormData();
      fd.append('image', file);
      await axiosClient.post(`/api/chat/cargo/${cargoId}/messages/image`, fd, {
        params: { channel },
        headers: { 'Content-Type': undefined },
      });
    } catch (err) {
      setError(err.response?.data?.message || t('chat.errSendImage'));
    } finally {
      setUploadingImage(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 320, border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: 10, background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: 6 }}>
        {loading && <p className="text-muted" style={{ fontSize: 12.5, textAlign: 'center' }}>{t('common.loading')}</p>}
        {!loading && messages.length === 0 && (
          <div className="text-muted" style={{ textAlign: 'center', fontSize: 12.5, marginTop: 20 }}>
            <MessageCircle size={22} style={{ opacity: 0.4, marginBottom: 6 }} />
            <div>{t('chat.emptyState')}</div>
          </div>
        )}
        {messages.map((m) => (
          <div key={m.id} style={{ display: 'flex', flexDirection: 'column', alignItems: m.mine ? 'flex-end' : 'flex-start' }}>
            <div
              style={{
                maxWidth: '78%',
                padding: m.imageUrl ? 5 : '7px 11px',
                borderRadius: 12,
                fontSize: 13,
                background: m.mine ? 'var(--primary)' : 'var(--surface)',
                color: m.mine ? '#fff' : 'var(--text)',
                border: m.mine ? 'none' : '1px solid var(--border)',
              }}
            >
              {!m.mine && (
                <div style={{ fontSize: 10.5, fontWeight: 700, opacity: 0.7, marginBottom: 2, marginLeft: m.imageUrl ? 4 : 0, marginTop: m.imageUrl ? 4 : 0 }}>
                  {m.senderName}
                </div>
              )}
              {m.imageUrl && (
                <img
                  src={`${API_BASE}${m.imageUrl}`}
                  alt={t('chat.imageAlt')}
                  onClick={() => setLightboxUrl(`${API_BASE}${m.imageUrl}`)}
                  style={{ maxWidth: 200, maxHeight: 200, borderRadius: 8, display: 'block', cursor: 'pointer', objectFit: 'cover' }}
                />
              )}
              {m.message && (
                <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', padding: m.imageUrl ? '6px 4px 2px' : 0 }}>{m.message}</div>
              )}
            </div>
            <span className="text-muted" style={{ fontSize: 10, marginTop: 2 }}>{formatTime(m.createdAt, i18n.language)}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSend} style={{ display: 'flex', gap: 6, padding: 8, borderTop: '1px solid var(--border)', background: 'var(--surface)' }}>
        <input ref={fileInputRef} type="file" accept="image/*" onChange={handleImagePick} style={{ display: 'none' }} />
        <button
          type="button"
          className="btn btn-sm"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadingImage}
          style={{ padding: '7px 10px' }}
          title={t('chat.sendImageTitle')}
        >
          <ImageIcon size={14} />
        </button>
        <input
          className="input"
          style={{ flex: 1, fontSize: 13, padding: '7px 10px' }}
          placeholder={t('chat.messagePlaceholder')}
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={2000}
        />
        <button type="submit" className="btn btn-sm btn-primary" disabled={!text.trim() || sending} style={{ padding: '7px 10px' }}>
          <Send size={14} />
        </button>
      </form>
      {uploadingImage && <p className="text-muted" style={{ fontSize: 11, padding: '0 8px 6px' }}>{t('chat.sendingImage')}</p>}
      {error && <p style={{ color: 'var(--danger)', fontSize: 11, padding: '0 8px 6px' }}>{error}</p>}

      {lightboxUrl && <PhotoLightbox photos={[lightboxUrl]} onClose={() => setLightboxUrl(null)} />}
    </div>
  );
}
