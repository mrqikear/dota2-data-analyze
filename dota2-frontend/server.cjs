const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 5200;
const BACKEND = 'http://localhost:9601';
const DIST = path.join(__dirname, 'dist');

const MIME = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
};

const server = http.createServer((req, res) => {
  // Proxy /api and /asset to backend
  if (req.url.startsWith('/api') || req.url.startsWith('/asset')) {
    const options = {
      hostname: 'localhost',
      port: 9601,
      path: req.url.replace('/api', ''),
      method: req.method,
      headers: { ...req.headers, host: 'localhost:9601' }
    };
    const proxyReq = http.request(options, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });
    proxyReq.on('error', () => { res.writeHead(502); res.end('Proxy Error'); });
    req.pipe(proxyReq);
    return;
  }

  // Serve static files from dist
  let filePath = path.join(DIST, req.url === '/' ? 'index.html' : req.url);
  fs.readFile(filePath, (err, data) => {
    if (err) {
      // SPA fallback
      fs.readFile(path.join(DIST, 'index.html'), (err2, data2) => {
        if (err2) { res.writeHead(404); res.end('Not Found'); return; }
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(data2);
      });
      return;
    }
    const ext = path.extname(filePath);
    res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
    res.end(data);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Server running at http://localhost:' + PORT);
});
