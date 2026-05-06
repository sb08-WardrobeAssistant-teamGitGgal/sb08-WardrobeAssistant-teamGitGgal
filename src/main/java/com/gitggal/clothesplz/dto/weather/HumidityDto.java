package com.gitggal.clothesplz.dto.weather;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JSON 변환을 위한 기본 생성자 (안전하게 protected)
@AllArgsConstructor
public class HumidityDto {

    private double current;
    private double comparedToDayBefore;

}
