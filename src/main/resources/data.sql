-- 카테고리
INSERT INTO category (name, slug) VALUES ('OTT', 'ott');
INSERT INTO category (name, slug) VALUES ('음악', 'music');
INSERT INTO category (name, slug) VALUES ('쇼핑 멤버십', 'shopping');
INSERT INTO category (name, slug) VALUES ('AI·생산성', 'ai');
INSERT INTO category (name, slug) VALUES ('배달·생활', 'life');
INSERT INTO category (name, slug) VALUES ('기타', 'etc');

-- 구독 서비스
INSERT INTO subscription (category_id, name, slug, brand_color, refund_info, verified_at, source, channels, difficulty, official_cancel_url, estimated_minutes, caution)
VALUES (1, '넷플릭스', 'netflix', '#E50914', '해지해도 다음 결제일 전까지 이용할 수 있어요.', '2026-11-12', 'ADMIN', 'WEB', 'EASY', 'https://www.netflix.com/cancelplan', 3, '앱스토어나 구글플레이로 결제했다면 그쪽 구독 관리에서 해지해야 해요.');
INSERT INTO subscription (category_id, name, slug, brand_color, refund_info, verified_at, source, channels, difficulty, official_cancel_url, estimated_minutes, caution)
VALUES (1, '티빙', 'tving', '#FF153C', '결제 후 이용 이력이 없으면 환불을 요청할 수 있어요.', '2026-11-12', 'ADMIN', 'APP,WEB', 'NORMAL', NULL, 5, NULL);
INSERT INTO subscription (category_id, name, slug, brand_color, refund_info, verified_at, source, channels, difficulty, official_cancel_url, estimated_minutes, caution)
VALUES (2, '멜론', 'melon', '#00CD3C', '해지하면 다음 결제일부터 요금이 나가지 않아요.', '2026-11-12', 'ADMIN', NULL, NULL, NULL, NULL, NULL);

-- 넷플릭스 해지 단계
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (1, 1, '홈', '프로필 아이콘', 'top-right', '오른쪽 위 프로필 사진을 눌러요.');
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (1, 2, '프로필', '계정', 'middle', '메뉴에서 계정을 눌러요.');
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (1, 3, '계정', '멤버십 해지', 'middle', '멤버십 해지 버튼을 누르고 확인해요.');

-- 멜론 해지 단계
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (3, 1, '홈', '내 정보', 'bottom', '아래쪽 메뉴에서 내 정보를 눌러요.');
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (3, 2, '내 정보', '이용권 관리', 'middle', '이용권 관리를 눌러요.');
INSERT INTO guide_step (subscription_id, step_order, screen_name, target_label, position, description)
VALUES (3, 3, '이용권 관리', '해지 신청', 'bottom', '해지 신청을 누르고 확인해요.');