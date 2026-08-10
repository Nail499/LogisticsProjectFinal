import axiosClient from '../api/axiosClient';

// Brauzer push bildirişləri — Push API + VAPID (bax public/sw.js,
// backend PushNotificationService/PushSubscriptionController).

// PushManager.subscribe VAPID açarını Uint8Array kimi gözləyir, backend isə
// base64url mətn kimi göndərir — standart çevirmə (MDN-in özündə tövsiyə
// olunan metod).
function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)));
}

export function isPushSupported() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
}

export async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return null;
  return navigator.serviceWorker.register('/sw.js');
}

export async function getCurrentSubscription() {
  if (!isPushSupported()) return null;
  const registration = await navigator.serviceWorker.ready.catch(() => null);
  if (!registration) return null;
  return registration.pushManager.getSubscription();
}

export async function subscribeToPush() {
  if (!isPushSupported()) throw new Error('Bu brauzer push bildirişlərini dəstəkləmir');

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') throw new Error('Bildiriş icazəsi verilmədi');

  await registerServiceWorker();
  const registration = await navigator.serviceWorker.ready;

  const { data } = await axiosClient.get('/api/push/vapid-public-key');
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(data.publicKey),
  });

  await axiosClient.post('/api/push/subscribe', subscription.toJSON());
  return subscription;
}

export async function unsubscribeFromPush() {
  const subscription = await getCurrentSubscription();
  if (!subscription) return;
  await axiosClient.post('/api/push/unsubscribe', { endpoint: subscription.endpoint });
  await subscription.unsubscribe();
}
