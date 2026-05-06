package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PrecipitationDto {

    private PrecipitationType type; // Enum 사용
    private Double amount;          // 강수량
    private Double probability;     // 강수 확률

}