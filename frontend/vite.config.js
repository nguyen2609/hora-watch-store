import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backendOrigin = 'http://localhost:8080';
const frontendHosts = new Set(['localhost:5173', '127.0.0.1:5173']);

// Validate the browser origin BEFORE mapping it to Java's configured origin.
// This keeps Java's Origin/Host/CSRF checks intact without enabling CORS.
function localApiBoundary() {
  function guard(req, res, next) {
    if (!/^\/api(?:\/|$)/.test(req.url)) return next();

    const host = req.headers.host;
    const origin = req.headers.origin;
    const site = req.headers['sec-fetch-site'];
    if (
      !frontendHosts.has(host) ||
      (origin && origin !== `http://${host}`) ||
      (site && site !== 'same-origin' && site !== 'none')
    ) {
      res.writeHead(403, { 'Content-Type': 'application/json; charset=UTF-8' });
      res.end(JSON.stringify({ code: 'ORIGIN_REJECTED', message: 'Cross-origin requests are not permitted.' }));
      return;
    }
    next();
  }

  return {
    name: 'local-api-boundary',
    configureServer(server) { server.middlewares.use(guard); },
    configurePreviewServer(server) { server.middlewares.use(guard); },
  };
}

const proxy = {
  '/api': {
    // Connect over IPv4 because Java binds to 127.0.0.1 in local development.
    target: 'http://127.0.0.1:8080',
    configure(proxyServer) {
      proxyServer.on('proxyReq', (proxyReq, req) => {
        proxyReq.setHeader('Host', new URL(backendOrigin).host);
        if (req.headers.origin) proxyReq.setHeader('Origin', backendOrigin);
      });
    },
  },
};

export default defineConfig({
  plugins: [react(), localApiBoundary()],
  server: { host: '127.0.0.1', port: 5173, strictPort: true, cors: false, proxy },
  preview: { host: '127.0.0.1', port: 5173, strictPort: true, cors: false, proxy },
});
