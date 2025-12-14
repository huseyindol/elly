import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

// STRESS TEST - Sistemi limitine kadar zorla
export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Warm-up
    { duration: '2m', target: 100 },   // Normal load
    { duration: '2m', target: 200 },   // High load
    { duration: '2m', target: 300 },   // Very high load
    { duration: '2m', target: 400 },   // Extreme load
    { duration: '3m', target: 500 },   // STRESS - Find breaking point
    { duration: '1m', target: 0 },     // Recovery
  ],
  thresholds: {
    http_req_duration: ['p(99)<2000'], // Daha toleranslı
    http_req_failed: ['rate<0.1'],     // %10'a kadar hata kabul edilebilir
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Ağır bir senaryo - JOIN'li complex query
  const scenarios = [
    '/api/pages',
    '/api/posts',
    '/api/components',
    '/api/widgets',
    '/api/banners',
    '/api/comments?postId=1',
    '/api/ratings/stats?postId=1',
  ];

  const randomEndpoint = scenarios[Math.floor(Math.random() * scenarios.length)];
  
  const response = http.get(`${BASE_URL}${randomEndpoint}`);
  
  check(response, {
    'Status is 200, 404, or 500': (r) => [200, 404, 500].includes(r.status),
    'Response time < 5000ms': (r) => r.timings.duration < 5000,
  }) || errorRate.add(1);

  // Stress test'te daha az bekleme
  sleep(Math.random() * 0.5);
}
