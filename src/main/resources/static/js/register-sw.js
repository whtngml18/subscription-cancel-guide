// 브라우저가 서비스 워커를 지원하면 등록한다 (PWA: 홈 화면 추가, 오프라인 화면)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/service-worker.js').catch((error) => {
            console.warn('서비스 워커 등록 실패', error);
        });
    });
}
