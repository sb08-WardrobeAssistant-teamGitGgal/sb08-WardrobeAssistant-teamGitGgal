package com.gitggal.clothesplz.dto.weather;


import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TemperatureDto {

    private double current;
    private double comparedToDayBefore;
    private double min;
    private double max;


}
