-- =============================================
-- V1.1 dummy data
-- =============================================

-- users
INSERT INTO "users" ("id", "email", "name", "password", "created_at", "updated_at", "role", "locked",
                     "temp_password", "temp_password_expires_at")
VALUES ('11111111-1111-1111-1111-111111111111', 'alice@example.com', 'Alice', 'encoded-password-1', NOW(), NOW(),
        'USER', FALSE, NULL, NULL),
       ('22222222-2222-2222-2222-222222222222', 'bob@example.com', 'Bob', 'encoded-password-2', NOW(), NOW(), 'USER',
        FALSE, NULL, NULL),
       ('33333333-3333-3333-3333-333333333333', 'admin@example.com', 'Admin', 'encoded-password-3', NOW(), NOW(),
        'ADMIN', FALSE, NULL, NULL);

-- social_accounts
INSERT INTO "social_accounts" ("id", "user_id", "provider", "provider_id", "created_at")
VALUES ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', 'GOOGLE',
        'google-alice-001', NOW()),
       ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '22222222-2222-2222-2222-222222222222', 'KAKAO', 'kakao-bob-001',
        NOW());

-- profiles
INSERT INTO "profiles" ("id", "user_id", "image_url", "gender", "birth_date", "latitude", "longitude",
                        "temp_sensitivity", "grid_x", "grid_y", "created_at", "updated_at")
VALUES ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '11111111-1111-1111-1111-111111111111', 'https://img.example/alice',
        'FEMALE', '1995-04-12', 37.5665, 126.9780, 3, 60, 127, NOW(), NOW()),
       ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '22222222-2222-2222-2222-222222222222', 'https://img.example/bob',
        'MALE', '1993-11-03', 35.1796, 129.0756, 2, 98, 76, NOW(), NOW()),
       ('bbbbbbb3-bbbb-bbbb-bbbb-bbbbbbbbbbb3', '33333333-3333-3333-3333-333333333333', NULL, 'OTHER', '1990-01-01',
        37.4563, 126.7052, 4, 55, 124, NOW(), NOW());

-- locations
INSERT INTO "locations" ("id", "latitude", "longitude", "grid_x", "grid_y", "location_names", "created_at", "updated_at")
VALUES ('44444444-4444-4444-4444-444444444444', 37.5665, 126.9780, 60, 127, 'Seoul,Jung-gu', NOW(), NOW()),
       ('55555555-5555-5555-5555-555555555555', 35.1796, 129.0756, 98, 76, 'Busan,Jung-gu', NOW(), NOW());

-- weathers
INSERT INTO "weathers" ("id", "location_id", "forecasted_at", "forecast_at", "sky_status", "precipitation_type",
                        "precipitation_amount", "precipitation_probability", "humidity", "humidity_diff",
                        "temperature_current", "temperature_diff", "temperature_min", "temperature_max", "wind_speed",
                        "wind_phrase", "created_at", "updated_at")
VALUES ('66666666-6666-6666-6666-666666666666', '44444444-4444-4444-4444-444444444444', NOW(), NOW(), 'CLEAR', 'NONE',
        0.0, 10.0, 45.0, -5.0, 22.5, 1.2, 18.0, 25.0, 2.1, 'WEAK', NOW(), NOW()),
       ('77777777-7777-7777-7777-777777777777', '55555555-5555-5555-5555-555555555555', NOW(), NOW(), 'CLOUDY',
        'RAIN', 3.2, 70.0, 80.0, 8.0, 19.0, -0.7, 16.5, 21.0, 6.5, 'MODERATE', NOW(), NOW());

-- feeds
INSERT INTO "feeds" ("id", "content", "ootds", "created_at", "updated_at", "like_count", "comment_count", "author_id",
                     "weather_id")
VALUES ('88888888-8888-8888-8888-888888888888', '오늘 코디 공유해요', '[{"type":"TOP","name":"화이트 셔츠"}]'::jsonb, NOW(),
        NOW(), 1, 1, '11111111-1111-1111-1111-111111111111', '66666666-6666-6666-6666-666666666666');

-- feed_likes
INSERT INTO "feed_likes" ("id", "created_at", "feed_id", "user_id")
VALUES ('99999999-9999-9999-9999-999999999999', NOW(), '88888888-8888-8888-8888-888888888888',
        '22222222-2222-2222-2222-222222222222');

-- feed_comments
INSERT INTO "feed_comments" ("id", "content", "created_at", "feed_id", "author_id")
VALUES ('aaaaaaaa-1111-2222-3333-aaaaaaaaaaaa', '코디 좋아요!', NOW(), '88888888-8888-8888-8888-888888888888',
        '22222222-2222-2222-2222-222222222222');

-- follows
INSERT INTO "follows" ("id", "follower_id", "followee_id", "created_at")
VALUES ('bbbbbbbb-1111-2222-3333-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222',
        '11111111-1111-1111-1111-111111111111', NOW());

-- direct_messages
INSERT INTO "direct_messages" ("id", "sender_id", "receiver_id", "content", "created_at")
VALUES ('cccccccc-1111-2222-3333-cccccccccccc', '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222', '안녕하세요!', NOW());

-- notifications
INSERT INTO "notifications" ("id", "receiver_id", "title", "content", "level", "created_at")
VALUES ('dddddddd-1111-2222-3333-dddddddddddd', '11111111-1111-1111-1111-111111111111', '새 댓글 알림',
        '회원님의 피드에 댓글이 달렸습니다.', 'INFO', NOW());

-- clothes
INSERT INTO "clothes" ("id", "owner_id", "name", "type", "image_url", "purchase_url", "created_at", "updated_at")
VALUES ('eeeeeeee-1111-2222-3333-eeeeeeeeeeee', '11111111-1111-1111-1111-111111111111', '화이트 셔츠', 'TOP',
        'https://img.example/shirt', 'https://shop.example/shirt', NOW(), NOW());

-- clothes_attribute_def
INSERT INTO "clothes_attribute_def" ("id", "name", "selectable_values", "created_at", "updated_at")
VALUES ('ffffffff-1111-2222-3333-ffffffffffff', 'color', '["WHITE","BLACK","BLUE"]'::jsonb, NOW(), NOW());

-- clothes_attribute
INSERT INTO "clothes_attribute" ("id", "clothes_id", "definition_id", "value", "created_at", "updated_at")
VALUES ('12121212-3434-5656-7878-909090909090', 'eeeeeeee-1111-2222-3333-eeeeeeeeeeee',
        'ffffffff-1111-2222-3333-ffffffffffff', 'WHITE', NOW(), NOW());
