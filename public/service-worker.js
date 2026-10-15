self.addEventListener('push', (event) => {
  if (!event.data) return
  const message = event.data.json()
  event.waitUntil(self.registration.showNotification(message.title, {
    body: message.body,
    icon: '/katui-logo.png',
    badge: '/katui-logo.png',
    tag: message.tag,
    data: { url: message.url || '/' },
    requireInteraction: true,
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const target = new URL(event.notification.data?.url || '/', self.location.origin).href
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
    const existing = clients.find((client) => new URL(client.url).origin === self.location.origin)
    return existing ? existing.focus().then(() => existing.navigate(target)) : self.clients.openWindow(target)
  }))
})
