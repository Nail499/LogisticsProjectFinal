import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Stage 6: sockjs-client assumes a Node-style `global` object (it falls
  // back to SockJS's websocket transport via Node's `crypto`/`event`
  // modules), which doesn't exist in the browser bundle Vite produces.
  // Aliasing it to `globalThis` is the standard fix for this combo.
  define: {
    global: 'globalThis',
  },
})
