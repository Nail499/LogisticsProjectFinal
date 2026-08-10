// Fleetra — brauzer push bildirişləri üçün service worker (bax
// utils/push.js#registerServiceWorker, backend PushNotificationService).
// Yalnız iki hadisəni idarə edir: gələn push mesajını sistem bildirişi
// kimi göstərmək, və bildirişə klik edildikdə uyğun səhifəni açmaq/
// fokuslamaq. Vite tərəfindən bundle edilmir — kök "/sw.js" olaraq
// birbaşa statik servis edilir (public/ qovluğu).

self.addEventListener('push', (event) => {
  let data = { title: 'Fleetra', body: 'Yeni bildiriş', url: '/' };
  try {
    if (event.data) data = { ...data, ...event.data.json() };
  } catch {
    // JSON deyilsə (gözlənilməz), default mətnlə davam et.
  }

  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: '/favicon.svg',
      badge: '/favicon.svg',
      data: { url: data.url || '/' },
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.navigate(targetUrl);
          return client.focus();
        }
      }
      return clients.openWindow(targetUrl);
    })
  );
});
