-- =============================================
-- users
-- =============================================
CREATE TABLE "users"
(
    "id"                       UUID                       NOT NULL,
    "email"                    VARCHAR(255)               NOT NULL UNIQUE,
    "name"                     VARCHAR(20)                NOT NULL,
    "password"                 VARCHAR(255)               NOT NULL,
    "created_at"               TIMESTAMP WITH TIME ZONE   NOT NULL,
    "updated_at"               TIMESTAMP WITH TIME ZONE   NOT NULL,
    "role"                     VARCHAR(20) DEFAULT 'USER' NOT NULL, -- 수정: DEFAULT USER → DEFAULT 'USER'
    "locked"                   BOOLEAN     DEFAULT FALSE  NOT NULL,
    "temp_password"            VARCHAR(255)               NULL,
    "temp_password_expires_at" TIMESTAMP WITH TIME ZONE   NULL,
    CONSTRAINT "PK_USERS" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_users_role" CHECK ("role" IN ('USER', 'ADMIN'))
);

-- 잠금 계정 관리 쿼리 최적화 (locked=true인 사용자만 필터링할 때 유용)
CREATE INDEX "IDX_users_locked" ON "users" ("locked") WHERE "locked" = TRUE;

-- =============================================
-- social_accounts
-- =============================================
CREATE TABLE "social_accounts"
(
    "id"          UUID                     NOT NULL,
    "user_id"     UUID                     NOT NULL,
    "provider"    VARCHAR(20)              NOT NULL,
    "provider_id" VARCHAR(255)             NOT NULL,
    "created_at"  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_SOCIAL_ACCOUNTS" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_social_accounts_provider" CHECK ("provider" IN ('GOOGLE', 'KAKAO')),
    CONSTRAINT "FK_users_TO_social_accounts_1" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 소셜 로그인 시 provider + provider_id 조합으로 사용자 조회
CREATE UNIQUE INDEX "UQ_social_accounts_provider_provider_id" ON "social_accounts" ("provider", "provider_id");
-- user_id 기준 소셜 계정 목록 조회
CREATE INDEX "IDX_social_accounts_user_id" ON "social_accounts" ("user_id");

-- =============================================
-- profiles
-- =============================================
CREATE TABLE "profiles"
(
    "id"               UUID             NOT NULL,
    "user_id"          UUID             NOT NULL,
    "image_url"        VARCHAR          NULL,
    "gender"           VARCHAR(10)      NULL,
    "birth_date"       DATE             NULL,
    "latitude"         DOUBLE PRECISION NULL,
    "longitude"        DOUBLE PRECISION NULL,
    "temp_sensitivity" SMALLINT         NULL,
    "grid_x"           INTEGER          NULL,
    "grid_y"           INTEGER          NULL,
    "created_at"     TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_PROFILES" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_profiles_gender" CHECK ("gender" IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT "CHK_profiles_temp_sensitivity" CHECK ("temp_sensitivity" BETWEEN 1 AND 5),
    CONSTRAINT "FK_users_TO_profiles_1" FOREIGN KEY ("user_id") REFERENCES "users" ("id")
);

-- 1유저 1프로필 보장
CREATE UNIQUE INDEX "UQ_profiles_user_id" ON "profiles" ("user_id");

-- =============================================
-- locations
-- =============================================
CREATE TABLE "locations"
(
    "id"             UUID                     NOT NULL,
    "latitude"       DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "longitude"      DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "grid_x"         INTEGER                  NOT NULL, -- 수정: "X" → grid_x (대문자 컬럼명은 항상 따옴표 필요해 실수 유발)
    "grid_y"         INTEGER                  NOT NULL, -- 수정: "Y" → grid_y
    "location_names" VARCHAR(255)             NOT NULL,
    "created_at"     TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_LOCATIONS" PRIMARY KEY ("id")
);

-- 좌표로 위치 검색 시 사용 (기상청 grid 좌표 조회)
CREATE INDEX "IDX_locations_grid" ON "locations" ("grid_x", "grid_y");

-- =============================================
-- weathers
-- =============================================
CREATE TABLE "weathers"
(
    "id"                        UUID                     NOT NULL,
    "location_id"               UUID                     NOT NULL,
    "forecasted_at"             TIMESTAMP WITH TIME ZONE NOT NULL,
    "forecast_at"               TIMESTAMP WITH TIME ZONE NOT NULL,
    "sky_status"                VARCHAR(20)              NOT NULL, -- 수정: ENUM → VARCHAR + CHECK, 실제 값은 요구사항에 맞게 수정
    "precipitation_type"        VARCHAR(20)              NOT NULL, -- 수정: ENUM → VARCHAR + CHECK, 실제 값은 요구사항에 맞게 수정
    "precipitation_amount"      DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "precipitation_probability" DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "humidity"                  DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "humidity_diff"             DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "temperature_current"       DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "temperature_diff"          DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "temperature_min"           DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "temperature_max"           DOUBLE PRECISION         NOT NULL, -- 수정: DOUBLE → DOUBLE PRECISION
    "wind_speed"                DOUBLE PRECISION         NOT NULL, -- 수정: wind_Speed → wind_speed, DOUBLE → DOUBLE PRECISION
    "wind_phrase"               VARCHAR(20)              NOT NULL, -- 수정: ENUM → VARCHAR + CHECK, 실제 값은 요구사항에 맞게 수정
    "created_at"                TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"                TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_WEATHERS" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_weathers_sky_status" CHECK ("sky_status" IN ('CLEAR', 'MOSTLY_CLOUDY', 'CLOUDY')),
    CONSTRAINT "CHK_weathers_precipitation" CHECK ("precipitation_type" IN
                                                   ('NONE', 'RAIN', 'RAIN_SNOW', 'SNOW', 'SHOWER')),
    CONSTRAINT "CHK_weathers_wind_phrase" CHECK ("wind_phrase" IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT "FK_locations_TO_weathers_1" FOREIGN KEY ("location_id") REFERENCES "locations" ("id")
);

-- 특정 위치의 최신 날씨 조회 (location_id + forecast_at 범위 쿼리)
CREATE INDEX "IDX_weathers_location_forecast" ON "weathers" ("location_id", "forecast_at");

-- =============================================
-- feeds
-- =============================================
CREATE TABLE "feeds"
(
    "id"            UUID                     NOT NULL,
    "content"       TEXT                     NOT NULL,
    "ootds"         JSONB                    NOT NULL,
    "created_at"    TIMESTAMP WITH TIME ZONE NOT NULL, -- 수정: TIMESTAMPZ → TIMESTAMP WITH TIME ZONE
    "updated_at"    TIMESTAMP WITH TIME ZONE NOT NULL, -- 수정: TIMESTAMPZ → TIMESTAMP WITH TIME ZONE
    "like_count"    BIGINT                   NOT NULL,
    "comment_count" BIGINT                   NOT NULL,
    "author_id"     UUID                     NOT NULL,
    "weather_id"    UUID                     NOT NULL,
    CONSTRAINT "PK_FEEDS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_users_TO_feeds_1" FOREIGN KEY ("author_id") REFERENCES "users" ("id"),
    CONSTRAINT "FK_weathers_TO_feeds_1" FOREIGN KEY ("weather_id") REFERENCES "weathers" ("id")
);

-- 특정 유저의 피드 목록 최신순 조회
CREATE INDEX "IDX_feeds_author_created" ON "feeds" ("author_id", "created_at" DESC);
-- 전체 피드 최신순 정렬 (홈 피드)
CREATE INDEX "IDX_feeds_created_at" ON "feeds" ("created_at" DESC);

-- =============================================
-- feed_likes
-- =============================================
CREATE TABLE "feed_likes"
(
    "id"         UUID                     NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,                     -- 수정: TIMESTAMPZ → TIMESTAMP WITH TIME ZONE
    "feed_id"    UUID                     NOT NULL,
    "user_id"    UUID                     NOT NULL,
    CONSTRAINT "PK_FEED_LIKES" PRIMARY KEY ("id"),
    CONSTRAINT "UQ_feed_likes_feed_user" UNIQUE ("feed_id", "user_id"), -- 중복 좋아요 방지
    CONSTRAINT "FK_feeds_TO_feed_likes_1" FOREIGN KEY ("feed_id") REFERENCES "feeds" ("id"),
    CONSTRAINT "FK_users_TO_feed_likes_1" FOREIGN KEY ("user_id") REFERENCES "users" ("id")
);

-- 특정 유저가 좋아요한 피드 목록 조회
CREATE INDEX "IDX_feed_likes_user_id" ON "feed_likes" ("user_id");

-- =============================================
-- feed_comments
-- =============================================
CREATE TABLE "feed_comments"
(
    "id"         UUID                     NOT NULL,
    "content"    TEXT                     NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, -- 수정: TIMESTAMPZ → TIMESTAMP WITH TIME ZONE
    "feed_id"    UUID                     NOT NULL,
    "author_id"  UUID                     NOT NULL,
    CONSTRAINT "PK_FEED_COMMENTS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_feeds_TO_feed_comments_1" FOREIGN KEY ("feed_id") REFERENCES "feeds" ("id"),
    CONSTRAINT "FK_users_TO_feed_comments_1" FOREIGN KEY ("author_id") REFERENCES "users" ("id")
);

-- 특정 피드의 댓글 목록 최신순 조회
CREATE INDEX "IDX_feed_comments_feed_created" ON "feed_comments" ("feed_id", "created_at" DESC);

-- =============================================
-- follows
-- =============================================
CREATE TABLE "follows"
(
    "id"          UUID                                   NOT NULL,
    "follower_id" UUID                                   NOT NULL,
    "followee_id" UUID                                   NOT NULL,
    "created_at"  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,                   -- 수정: DEFAULT TIMESTAMP WITH TIME ZONE → DEFAULT now()
    CONSTRAINT "PK_FOLLOWS" PRIMARY KEY ("id"),
    CONSTRAINT "UQ_follows_follower_followee" UNIQUE ("follower_id", "followee_id"), -- 중복 팔로우 방지
    CONSTRAINT "FK_users_TO_follows_1" FOREIGN KEY ("follower_id") REFERENCES "users" ("id"),
    CONSTRAINT "FK_users_TO_follows_2" FOREIGN KEY ("followee_id") REFERENCES "users" ("id")
);

-- 팔로잉 목록 조회 (내가 팔로우하는 사람)
CREATE INDEX "IDX_follows_follower_id" ON "follows" ("follower_id");
-- 팔로워 목록 조회 (나를 팔로우하는 사람)
CREATE INDEX "IDX_follows_followee_id" ON "follows" ("followee_id");

-- =============================================
-- direct_messages
-- =============================================
CREATE TABLE "direct_messages"
(
    "id"          UUID                     NOT NULL,
    "sender_id"   UUID                     NOT NULL,
    "receiver_id" UUID                     NOT NULL,
    "content"     VARCHAR(100)             NOT NULL,
    "created_at"  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_DIRECT_MESSAGES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_users_TO_direct_messages_1" FOREIGN KEY ("sender_id") REFERENCES "users" ("id"),
    CONSTRAINT "FK_users_TO_direct_messages_2" FOREIGN KEY ("receiver_id") REFERENCES "users" ("id")
);

-- 두 유저 간의 대화 내역 시간순 조회
CREATE INDEX "IDX_dm_sender_receiver_created" ON "direct_messages" ("sender_id", "receiver_id", "created_at" DESC);
-- 내가 받은 메시지 조회
CREATE INDEX "IDX_dm_receiver_created" ON "direct_messages" ("receiver_id", "created_at" DESC);

-- =============================================
-- notifications
-- =============================================
CREATE TABLE "notifications"
(
    "id"          UUID                     NOT NULL,
    "receiver_id" UUID                     NOT NULL,
    "title"       VARCHAR(50)              NOT NULL,
    "content"     VARCHAR(100)             NULL,
    "level"       VARCHAR(10)              NOT NULL,
    "created_at"  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_NOTIFICATIONS" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_notifications_level" CHECK ("level" IN ('INFO', 'WARNING', 'ERROR')),
    CONSTRAINT "FK_users_TO_notifications_1" FOREIGN KEY ("receiver_id") REFERENCES "users" ("id")
);

-- 특정 유저의 알림 최신순 조회
CREATE INDEX "IDX_notifications_receiver_created" ON "notifications" ("receiver_id", "created_at" DESC);

-- =============================================
-- clothes
-- =============================================
CREATE TABLE "clothes"
(
    "id"           UUID                     NOT NULL,
    "owner_id"     UUID                     NOT NULL,
    "name"         VARCHAR(100)             NOT NULL,
    "type"         VARCHAR(20)              NOT NULL, -- 실제 값은 요구사항에 맞게 수정
    "image_url"    VARCHAR                  NULL,
    "purchase_url" VARCHAR                  NULL,
    "created_at"   TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_CLOTHES" PRIMARY KEY ("id"),
    CONSTRAINT "CHK_clothes_type" CHECK ("type" IN ('TOP', 'BOTTOM', 'DRESS', 'OUTER', 'UNDERWEAR',
                                                    'ACCESSORY', 'SHOES', 'SOCKS', 'HAT', 'BAG',
                                                    'SCARF', 'ETC')),
    CONSTRAINT "FK_users_TO_clothes_1" FOREIGN KEY ("owner_id") REFERENCES "users" ("id")
);

-- 특정 유저의 옷 목록 조회
CREATE INDEX "IDX_clothes_owner_id" ON "clothes" ("owner_id");
-- 옷 종류별 필터링
CREATE INDEX "IDX_clothes_owner_type" ON "clothes" ("owner_id", "type");

-- =============================================
-- clothes_attribute_def
-- =============================================
CREATE TABLE "clothes_attribute_def"
(
    "id"                UUID                     NOT NULL,
    "name"              VARCHAR                  NOT NULL,
    "selectable_values" JSONB                    NOT NULL, -- 수정: JSON → JSONB (인덱싱/검색 성능 우위)
    "created_at"        TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_CLOTHES_ATTRIBUTE_DEF" PRIMARY KEY ("id")
);

-- =============================================
-- clothes_attribute
-- =============================================
CREATE TABLE "clothes_attribute"
(
    "id"            UUID                     NOT NULL,
    "clothes_id"    UUID                     NOT NULL,
    "definition_id" UUID                     NOT NULL,
    "value"         VARCHAR                  NOT NULL,
    "created_at"    TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "PK_CLOTHES_ATTRIBUTE" PRIMARY KEY ("id"),
    CONSTRAINT "UQ_clothes_attribute_clothes_definition" UNIQUE ("clothes_id", "definition_id"), -- 같은 옷에 동일 속성 중복 방지
    CONSTRAINT "FK_clothes_TO_clothes_attribute_1" FOREIGN KEY ("clothes_id") REFERENCES "clothes" ("id"),
    CONSTRAINT "FK_clothes_attribute_def_TO_clothes_attribute_1" FOREIGN KEY ("definition_id") REFERENCES "clothes_attribute_def" ("id")
);

-- 특정 옷의 속성 목록 조회
CREATE INDEX "IDX_clothes_attribute_clothes_id" ON "clothes_attribute" ("clothes_id");