-- 배포용 초기 데이터. 앱이 켜질 때마다 실행된다.
-- 카테고리는 표가 비어 있을 때(첫 배포)만 넣는다. 관리자가 지운 기본 카테고리가 다시 생기지 않게 한다.
-- 가이드는 관리자 화면에서 실제 해지 방법을 확인해 올린다.
INSERT INTO category (name, slug)
SELECT name, slug FROM (
    SELECT 1 AS ord, 'OTT' AS name, 'ott' AS slug
    UNION ALL SELECT 2, '음악', 'music'
    UNION ALL SELECT 3, '쇼핑 멤버십', 'shopping'
    UNION ALL SELECT 4, 'AI·생산성', 'ai'
    UNION ALL SELECT 5, '배달·생활', 'life'
    UNION ALL SELECT 6, '기타', 'etc'
) AS defaults
WHERE NOT EXISTS (SELECT 1 FROM category)
ORDER BY ord;
