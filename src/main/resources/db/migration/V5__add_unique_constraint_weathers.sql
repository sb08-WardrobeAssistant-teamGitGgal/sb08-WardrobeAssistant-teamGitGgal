-- 중복 rows 제거: (location_id, forecast_at) 기준으로 가장 오래된 row만 남김
DELETE FROM "weathers"
WHERE id NOT IN (
    SELECT MIN(id::text)::uuid
    FROM "weathers"
    GROUP BY location_id, forecast_at
);

-- (location_id, forecast_at) 유니크 제약조건 추가 — 동시 insert 중복 방지
ALTER TABLE "weathers"
    ADD CONSTRAINT "uk_weathers_location_forecast_at" UNIQUE (location_id, forecast_at);
