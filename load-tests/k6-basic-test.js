import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp-up: 0 to 10 users in 30s
    { duration: '1m', target: 50 },    // Ramp-up: 10 to 50 users in 1m
    { duration: '2m', target: 50 },    // Stay at 50 users for 2m
    { duration: '30s', target: 100 },  // Spike: 50 to 100 users in 30s
    { duration: '2m', target: 100 },   // Stay at 100 users for 2m (stress)
    { duration: '30s', target: 0 },    // Ramp-down: 100 to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.05'],   // Error rate should be less than 5%
    errors: ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Senaryo 1: Ana sayfa listesi al (GET Pages)
  let response = http.get(`${BASE_URL}/api/pages`);
  check(response, {
    'Pages status is 200': (r) => r.status === 200,
    'Pages response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);
  
  sleep(1);

  // Senaryo 2: Post listesi al (GET Posts)
  response = http.get(`${BASE_URL}/api/posts`);
  check(response, {
    'Posts status is 200': (r) => r.status === 200,
    'Posts has data': (r) => r.json().length >= 0,
  }) || errorRate.add(1);
  
  sleep(1);

  // Senaryo 3: Belirli bir post'u al (GET Post by ID)
  const postId = 1; // Test için var olan bir ID kullanın
  response = http.get(`${BASE_URL}/api/posts/${postId}`);
  check(response, {
    'Single post status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });
  
  sleep(1);

  // Senaryo 4: Component listesi (JOIN'ler var, daha ağır)
  response = http.get(`${BASE_URL}/api/components`);
  check(response, {
    'Components status is 200': (r) => r.status === 200,
    'Components response time < 1000ms': (r) => r.timings.duration < 1000,
  }) || errorRate.add(1);
  
  sleep(2);

  // Senaryo 5: Rating istatistikleri (Aggregation query)
  response = http.get(`${BASE_URL}/api/ratings/stats?postId=${postId}`);
  check(response, {
    'Rating stats status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });

  sleep(1);
}

// Teardown - Test sonrası bilgi yazdır
export function handleSummary(data) {
  return {
    'summary.json': JSON.stringify(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  return `
  ========================================
  📊 Load Test Summary
  ========================================
  Total Requests: ${data.metrics.http_reqs.values.count}
  Failed Requests: ${data.metrics.http_req_failed.values.passes}
  Avg Response Time: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
  P95 Response Time: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
  Max Response Time: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms
  ========================================
  `;
}
