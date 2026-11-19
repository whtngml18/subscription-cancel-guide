// 서비스 워커: 인터넷이 끊겨도 안내 화면을 보여주고, CSS·아이콘을 빠르게 불러온다.
// 저장할 파일이 바뀌면 CACHE_NAME의 숫자를 올린다. 그러면 예전 저장본은 지워진다.
const CACHE_NAME = 'cancel-guide-v1';
const OFFLINE_URL = '/offline.html';

const PRECACHE_URLS = [
    OFFLINE_URL,
    '/css/app.css',
    '/css/phone.css',
    '/icons/icon-192.png',
    '/icons/favicon-32.png',
    '/manifest.json'
];

// 설치: 오프라인 화면과 기본 파일을 미리 저장
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => cache.addAll(PRECACHE_URLS))
            .then(() => self.skipWaiting())
    );
});

// 활성화: 이름이 다른 예전 저장본 지우기
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((names) => Promise.all(
                names.filter((name) => name !== CACHE_NAME).map((name) => caches.delete(name))
            ))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = new URL(request.url);

    // 다른 사이트(CDN 등), GET이 아닌 요청(폼 제출), 관리자 화면은 그대로 둔다
    if (url.origin !== self.location.origin || request.method !== 'GET' || url.pathname.startsWith('/admin')) {
        return;
    }

    // 화면(HTML): 네트워크를 먼저 쓰고, 실패하면 오프라인 화면
    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request).catch(() => caches.match(OFFLINE_URL))
        );
        return;
    }

    // CSS·JS·아이콘: 저장본을 먼저 보여주고, 뒤에서 새 파일로 저장본을 바꾼다
    if (/^\/(css|js|icons)\//.test(url.pathname)) {
        event.respondWith(
            caches.open(CACHE_NAME).then((cache) =>
                cache.match(request).then((cached) => {
                    const network = fetch(request)
                        .then((response) => {
                            if (response.ok) {
                                cache.put(request, response.clone());
                            }
                            return response;
                        })
                        .catch(() => cached);
                    return cached || network;
                })
            )
        );
    }
});
