const redis = require('redis');

// Configuration with defaults
const REDIS_HOST = process.env.REDIS_HOST || 'redis';
const REDIS_PORT = process.env.REDIS_PORT || 6379;
const REDIS_URL = `redis://${REDIS_HOST}:${REDIS_PORT}`;

// Create Redis client
const client = redis.createClient({
  url: REDIS_URL,
  socket: {
    reconnectStrategy: (retries) => {
      // Exponential backoff for reconnection
      const delay = Math.min(retries * 100, 5000);
      console.log(`Redis reconnection attempt ${retries}, retrying in ${delay}ms`);
      return delay;
    }
  }
});

// Event handlers for better debugging
client.on('connect', () => console.log('Redis client connecting...'));
client.on('ready', () => console.log('Redis client ready'));
client.on('error', (err) => console.error('Redis Client Error:', err));
client.on('end', () => console.log('Redis client disconnected'));
client.on('reconnecting', () => console.log('Redis client reconnecting...'));

// Connection wrapper with retry logic
const connectWithRetry = async () => {
  try {
    await client.connect();
    console.log('Successfully connected to Redis');
  } catch (err) {
    console.error('Failed to connect to Redis:', err.message);
    console.log('Retrying connection in 5 seconds...');
    setTimeout(connectWithRetry, 5000);
  }
};

// Initialize connection
(async () => {
  await connectWithRetry();
  
  // Test the connection
  try {
    await client.ping();
    console.log('Redis ping successful');
  } catch (err) {
    console.error('Redis ping failed:', err);
  }
})();

// Graceful shutdown handler
process.on('SIGINT', async () => {
  console.log('Closing Redis connection...');
  await client.quit();
  process.exit();
});

module.exports = client;
