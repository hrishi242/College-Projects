const express = require('express');
const { isUri } = require('valid-url');
const client = require('./redisClient');
const generateShortCode = require('./generateShortCode');
const promClient = require('prom-client');

const app = express();
app.use(express.json());

// ===== 1. PROMETHEUS INIT =====
const collectDefaultMetrics = promClient.collectDefaultMetrics;
collectDefaultMetrics({ 
  timeout: 5000,
  register: promClient.register 
});

// Custom metrics
const httpRequestCounter = new promClient.Counter({
  name: 'http_requests_total',
  help: 'Total HTTP requests',
  labelNames: ['method', 'route', 'status']
});

const redisStatusGauge = new promClient.Gauge({
  name: 'redis_connected',
  help: 'Redis connection status'
});

const PORT = process.env.PORT || 3000;
const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

// ===== 2. REDIS CONNECTION MIDDLEWARE =====
app.use(async (req, res, next) => {
  if (!client.isOpen) {
    try {
      await client.connect();
      console.log('Redis reconnected');
    } catch (err) {
      console.error('Redis connection failed:', err);
      return res.status(500).json({ error: 'Redis disconnected' });
    }
  }
  next();
});

// ===== 3. ROUTES IN CORRECT ORDER =====

// Metrics endpoint (FIRST!)
app.get('/metrics', async (req, res) => {
  try {
    redisStatusGauge.set(client.isOpen ? 1 : 0);
    res.set('Content-Type', promClient.register.contentType);
    res.end(await promClient.register.metrics());
  } catch (err) {
    console.error('Metrics Error:', err);
    res.status(500).end('Error collecting metrics');
  }
});

// Health check
app.get('/health', async (req, res) => {
  try {
    redisStatusGauge.set(client.isOpen ? 1 : 0);
    res.json({ 
      status: 'OK',
      redis: client.isOpen ? 'connected' : 'disconnected'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Homepage
app.get('/', (req, res) => {
  res.send(`
    <h1>URL Shortener</h1>
    <p>Try these endpoints:</p>
    <ul>
      <li><a href="/health">/health</a></li>
      <li><a href="/metrics">/metrics</a></li>
    </ul>
  `);
});

// URL Shortening
app.post('/shorten', async (req, res) => {
  const { longUrl } = req.body;
  
  if (!longUrl) return res.status(400).json({ error: 'URL required' });
  if (!isUri(longUrl)) return res.status(400).json({ error: 'Invalid URL' });

  try {
    let shortCode;
    do {
      shortCode = generateShortCode();
    } while (await client.exists(shortCode));
    
    await client.set(shortCode, longUrl, { EX: 86400 });
    res.json({ 
      shortUrl: `${BASE_URL}/${shortCode}`,
      longUrl,
      expiresIn: '24h'
    });
  } catch (err) {
    console.error('Shorten Error:', err);
    res.status(500).json({ error: 'Internal error' });
  }
});

// Redirects (MUST BE LAST)
app.get('/:shortCode', async (req, res) => {
  try {
    const longUrl = await client.get(req.params.shortCode);
    if (longUrl) return res.redirect(longUrl);
    res.status(404).send('URL not found');
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===== 4. ERROR HANDLER =====
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal error' });
});

// ===== 5. START SERVER =====
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Redis: ${client.isOpen ? 'Connected' : 'Disconnected'}`);
});
