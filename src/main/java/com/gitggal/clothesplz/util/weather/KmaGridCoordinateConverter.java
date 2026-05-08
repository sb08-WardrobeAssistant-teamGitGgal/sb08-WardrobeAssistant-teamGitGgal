package com.gitggal.clothesplz.util.weather;

/**
 * 위도·경도(WGS84)를 기상청 단기예보 격자 좌표(nx, ny)로 변환합니다.
 */
public final class KmaGridCoordinateConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_RESOLUTION_KM = 5.0;
    /** 격자 변환에 쓰이는 정규화된 지구 반경 (기상청 안내 문서와 동일) */
    private static final double RE = EARTH_RADIUS_KM / GRID_RESOLUTION_KM;

    private static final double STD_LAT_1_DEG = 30.0;
    private static final double STD_LAT_2_DEG = 60.0;
    private static final double BASE_LON_DEG = 126.0;
    private static final double BASE_LAT_DEG = 38.0;

    private static final int X_OFFSET = 43;
    private static final int Y_OFFSET = 136;

    private static final double MIN_LAT_DEG = 33.0;
    private static final double MAX_LAT_DEG = 38.5;
    private static final double MIN_LON_DEG = 124.5;
    private static final double MAX_LON_DEG = 131.9;

    private KmaGridCoordinateConverter() {
    }

    public static KmaGridPoint toGrid(double latitudeDegrees, double longitudeDegrees) {
        if (latitudeDegrees < MIN_LAT_DEG
                || latitudeDegrees > MAX_LAT_DEG
                || longitudeDegrees < MIN_LON_DEG
                || longitudeDegrees > MAX_LON_DEG) {
            throw new IllegalArgumentException(
                    "한반도 범위를 벗어난 좌표입니다: lat=" + latitudeDegrees + ", lon=" + longitudeDegrees);
        }

        double degrad = Math.PI / 180.0;
        double slat1 = STD_LAT_1_DEG * degrad;
        double slat2 = STD_LAT_2_DEG * degrad;
        double olon = BASE_LON_DEG * degrad;
        double olat = BASE_LAT_DEG * degrad;

        double snRatio =
                Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(snRatio);

        double tanSlat1Half = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        double sf = Math.pow(tanSlat1Half, sn) * Math.cos(slat1) / sn;

        double ro = RE * sf / Math.tan(Math.PI * 0.25 + olat * 0.5);

        double tanLatHalf = Math.tan(Math.PI * 0.25 + latitudeDegrees * degrad * 0.5);
        double ra = RE * sf / Math.pow(tanLatHalf, sn);
        double theta = longitudeDegrees * degrad - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + X_OFFSET + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + Y_OFFSET + 0.5);

        return new KmaGridPoint(nx, ny);
    }

    public record KmaGridPoint(int nx, int ny) {
    }
}
