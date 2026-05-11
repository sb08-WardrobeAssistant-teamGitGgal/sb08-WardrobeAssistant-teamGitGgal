package com.gitggal.clothesplz.entity.weather;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "location",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grid_x_y", // 제약조건 이름
                        columnNames = {"grid_x", "grid_y"} // 중복을 허용하지 않을 컬럼들
                )
        }
)
public class Location extends BaseUpdatableEntity {

    @Column(nullable = false)
    private Double latitude;      // 위도 (DOUBLE PRECISION)

    @Column(nullable = false)
    private Double longitude;     // 경도 (DOUBLE PRECISION)

    @Column(name = "grid_x", nullable = false)
    private Integer gridX;        // 격자 X (INTEGER)

    @Column(name = "grid_y", nullable = false)
    private Integer gridY;        // 격자 Y (INTEGER)

    @Column(name = "location_names", nullable = false, length = 255)
    private String locationNames; // 행정 구역 명칭 (VARCHAR(255))
}