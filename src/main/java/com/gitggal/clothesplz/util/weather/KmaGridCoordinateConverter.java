package com.gitggal.clothesplz.util.weather;

/**
 * 위도·경도를 기상청 단기예보 격자 좌표(nx, ny)로 변환합니다.
 */
public final class KmaGridCoordinateConverter {

    private KmaGridCoordinateConverter() {
    }

    public static KmaGridPoint toGrid(double latitudeDegrees, double longitudeDegrees) {
        double re = 6371.00877 / 5.0;
        double slat1 = 30.0 * Math.PI / 180.0;
        double slat2 = 60.0 * Math.PI / 180.0;
        double olon = 126.0 * Math.PI / 180.0;
        double olat = 38.0 * Math.PI / 180.0;

        double sn =
                Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = re * sf / Math.tan(Math.PI * 0.25 + olat * 0.5);

        double degrad = Math.PI / 180.0;
        double ra =
                re * sf / Math.pow(Math.tan(Math.PI * 0.25 + latitudeDegrees * degrad * 0.5), sn);
        double theta = longitudeDegrees * degrad - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + 43 + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + 136 + 0.5);

        return new KmaGridPoint(nx, ny);
    }

    public record KmaGridPoint(int nx, int ny) {
    }
}
