package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.WindPhrase; // 방금 만드신 Enum 임포트
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WindSpeedDto {

    private Double speed;

    private WindPhrase asWord;

}
