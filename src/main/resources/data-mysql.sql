-- 배포용 초기 데이터. 앱이 켜질 때마다 실행되므로 INSERT IGNORE로 이미 있는 slug는 건너뛴다.
-- 가이드는 관리자 화면에서 실제 해지 방법을 확인해 올린다.
INSERT IGNORE INTO category (name, slug) VALUES ('OTT', 'ott');
INSERT IGNORE INTO category (name, slug) VALUES ('음악', 'music');
INSERT IGNORE INTO category (name, slug) VALUES ('쇼핑 멤버십', 'shopping');
