// Stage 6 — shared STOMP-over-SockJS client for the live GPS broadcast
// channel (see backend WebSocketConfig + TripBroadcastService). One
// connection is reused across the whole app; components subscribe/
// unsubscribe to individual topics without knowing about connection
// lifecycle, reconnects, etc.
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/ws';

let client = null;
const subscriptions = new Map();
let nextId = 1;

function ensureClient() {
  if (client) return client;

  client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  client.onConnect = () => {
    // (Re)apply every registered subscription — covers both the initial
    // connect and any reconnect after a dropped connection, since STOMP
    // subscriptions don't survive a reconnect on their own.
    subscriptions.forEach((entry) => {
      entry.stompSub = client.subscribe(entry.destination, (msg) => {
        try {
          entry.callback(JSON.parse(msg.body));
        } catch {
          /* ignore malformed payload */
        }
      });
    });
  };

  client.activate();
  return client;
}

// Subscribe to a topic (e.g. '/topic/dispatcher/live-trips' or
// '/topic/tracking/TRK123'). Safe to call before the connection is up.
// Returns an unsubscribe function — call it in a useEffect cleanup.
export function subscribeTopic(destination, callback) {
  const c = ensureClient();
  const id = nextId++;
  const entry = { destination, callback, stompSub: null };
  subscriptions.set(id, entry);

  if (c.connected) {
    entry.stompSub = c.subscribe(destination, (msg) => {
      try {
        callback(JSON.parse(msg.body));
      } catch {
        /* ignore malformed payload */
      }
    });
  }

  return () => {
    entry.stompSub?.unsubscribe();
    subscriptions.delete(id);
  };
}
