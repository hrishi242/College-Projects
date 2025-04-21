const autocannon = require('autocannon');
const { PassThrough } = require('stream');

function runTest() {
  const buf = [];
  const outputStream = new PassThrough();
  
  const instance = autocannon({
    url: 'http://localhost:3000',
    connections: 100,
    duration: 60,
    requests: [
      {
        method: 'POST',
        path: '/shorten',
        body: JSON.stringify({ longUrl: 'https://example.com' }),
        headers: {
          'Content-Type': 'application/json'
        }
      }
    ]
  }, (err, res) => {
    if (err) console.error(err);
    console.log(res);
  });

  autocannon.track(instance, { outputStream });
  outputStream.on('data', data => buf.push(data));
  process.on('exit', () => {
    process.stderr.write(Buffer.concat(buf));
  });
}

runTest();
