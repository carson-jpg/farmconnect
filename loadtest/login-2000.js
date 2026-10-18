// Load test for login. Install k6 (https://k6.io), then:
//   k6 run -e BASE=https://your-server -e EMAIL=you@example.com -e PASSWORD='YourPassword1' loadtest/login-2000.js
// Use a TEST server and a test account. Ramps to 2000 people logging in within about 10 seconds.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    burst: { executor: 'ramping-arrival-rate', startRate: 20, timeUnit: '1s', preAllocatedVUs: 2000, maxVUs: 2500,
      stages: [ { target: 200, duration: '10s' }, { target: 200, duration: '20s' }, { target: 0, duration: '5s' } ] },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],          // fewer than 1% failures
    http_req_duration: ['p(95)<3000'],       // 95% of logins finish within 3 seconds
  },
};

export default function () {
  const res = http.post(`${__ENV.BASE}/api/auth/login`,
    JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'logged in': (r) => r.status === 200 });
}
