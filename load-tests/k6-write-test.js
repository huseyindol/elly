import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// WRITE OPERATIONS TEST - POST, PUT, DELETE
export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const headers = {
    'Content-Type': 'application/json',
  };

  // Test 1: Create a new Post
  const postPayload = JSON.stringify({
    title: `Load Test Post ${Date.now()}`,
    content: 'This is a load test post created by K6',
    slug: `load-test-${Date.now()}`,
    publishedAt: new Date().toISOString(),
  });

  let response = http.post(`${BASE_URL}/api/posts`, postPayload, { headers });
  const postCreated = check(response, {
    'Post created successfully': (r) => r.status === 200 || r.status === 201,
  });

  let createdPostId;
  if (postCreated && response.status < 300) {
    try {
      const jsonResponse = response.json();
      createdPostId = jsonResponse.id || jsonResponse.data?.id;
    } catch (e) {
      console.error('Failed to parse post creation response');
    }
  }

  sleep(1);

  // Test 2: Create a Comment (if post was created)
  if (createdPostId) {
    const commentPayload = JSON.stringify({
      postId: createdPostId,
      author: `Tester ${Date.now()}`,
      content: 'This is a test comment',
      email: `test${Date.now()}@example.com`,
    });

    response = http.post(`${BASE_URL}/api/comments`, commentPayload, { headers });
    check(response, {
      'Comment created': (r) => r.status === 200 || r.status === 201,
    });

    sleep(1);

    // Test 3: Create a Rating
    const ratingPayload = JSON.stringify({
      postId: createdPostId,
      rating: Math.floor(Math.random() * 5) + 1, // 1-5 arası random
      userId: `user${Math.floor(Math.random() * 1000)}`,
    });

    response = http.post(`${BASE_URL}/api/ratings`, ratingPayload, { headers });
    check(response, {
      'Rating created': (r) => r.status === 200 || r.status === 201 || r.status === 409, // 409: duplicate
    });
  }

  sleep(2);

  // Test 4: Get all posts (Read-heavy operation after writes)
  response = http.get(`${BASE_URL}/api/posts`);
  check(response, {
    'Posts retrieved after write': (r) => r.status === 200,
  });

  sleep(1);
}
