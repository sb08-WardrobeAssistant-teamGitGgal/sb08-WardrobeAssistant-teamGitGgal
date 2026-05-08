-- 1. 테이블 이름 변경 (locations -> location)
ALTER TABLE "locations" RENAME TO "location";

-- 2. 기존 인덱스 이름 변경 (선택사항이지만 관례상 변경)
ALTER INDEX "IDX_locations_grid" RENAME TO "IDX_location_grid";

-- 3. 유니크 제약조건 추가 (grid_x, grid_y)
-- 기존에 중복된 데이터가 있다면 이 단계에서 에러가 날 수 있으므로 주의해야 합니다.
ALTER TABLE "location" ADD CONSTRAINT "uk_grid_x_y" UNIQUE ("grid_x", "grid_y");

-- 4. 외래 키 제약조건 이름 수정 (weathers 테이블에서 참조하는 이름)
-- 부모 테이블 이름이 바뀌었으므로 연관된 FK 설정도 확인이 필요합니다.
-- (PostgreSQL의 경우 RENAME TO 시 FK는 자동으로 따라오지만, 제약조건 이름은 수동으로 관리하는게 좋습니다.)
ALTER TABLE "weathers" RENAME CONSTRAINT "FK_locations_TO_weathers_1" TO "FK_location_TO_weathers_1";