-- 중복 rows 제거: (location_id, forecast_at) 그룹에서 created_at 기준 최초 row만 보존
DELETE FROM "weathers" w
USING (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY location_id, forecast_at
                   ORDER BY created_at ASC, id ASC
               ) AS rn
        FROM "weathers"
    ) t
    WHERE t.rn > 1
) d
WHERE w.id = d.id;

-- (location_id, forecast_at) 유니크 제약조건 추가 — 동시 insert 중복 방지
ALTER TABLE "weathers"
    ADD CONSTRAINT "uk_weathers_location_forecast_at" UNIQUE (location_id, forecast_at);
