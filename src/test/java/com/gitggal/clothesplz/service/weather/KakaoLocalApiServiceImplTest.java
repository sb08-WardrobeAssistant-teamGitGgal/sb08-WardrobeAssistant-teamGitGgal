package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.KakaoCoord2RegionResponseDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.service.weather.impl.KakaoLocalApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DisplayName("카카오 로컬 API 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class KakaoLocalApiServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private KakaoLocalApiServiceImpl kakaoLocalApiServiceImpl;

    @BeforeEach
    void setUp() {
        kakaoLocalApiServiceImpl = new KakaoLocalApiServiceImpl(webClient);
        ReflectionTestUtils.setField(kakaoLocalApiServiceImpl, "restApiKey", "test-api-key");

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(Function.class));
        doReturn(requestHeadersSpec).when(requestHeadersSpec).header(anyString(), anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(Predicate.class), any(Function.class));
    }

    @Test
    @DisplayName("행정동(H) 타입 응답이 있으면 시도/구/동 이름을 반환한다")
    void getLocationNames_withHType_returnsLocationNames() {
        // given
        KakaoCoord2RegionResponseDto response = new KakaoCoord2RegionResponseDto(
                new KakaoCoord2RegionResponseDto.Meta(2),
                List.of(
                        new KakaoCoord2RegionResponseDto.Document(
                                "B", "경기도 성남시 분당구 삼평동",
                                "경기도", "성남시 분당구", "삼평동", "", "4113510900", 127.1, 37.4),
                        new KakaoCoord2RegionResponseDto.Document(
                                "H", "경기도 성남시 분당구 삼평동",
                                "경기도", "성남시 분당구", "삼평동", "", "4113565500", 127.1, 37.4)
                )
        );
        when(responseSpec.bodyToMono(KakaoCoord2RegionResponseDto.class)).thenReturn(Mono.just(response));

        // when
        List<String> result = kakaoLocalApiServiceImpl.getLocationNames(37.4, 127.1).block();

        // then
        assertThat(result).containsExactly("경기도", "성남시 분당구", "삼평동");
    }

    @Test
    @DisplayName("법정동(B) 타입만 있으면 빈 리스트를 반환한다")
    void getLocationNames_withOnlyBType_returnsEmpty() {
        // given
        KakaoCoord2RegionResponseDto response = new KakaoCoord2RegionResponseDto(
                new KakaoCoord2RegionResponseDto.Meta(1),
                List.of(
                        new KakaoCoord2RegionResponseDto.Document(
                                "B", "경기도 성남시 분당구 삼평동",
                                "경기도", "성남시 분당구", "삼평동", "", "4113510900", 127.1, 37.4)
                )
        );
        when(responseSpec.bodyToMono(KakaoCoord2RegionResponseDto.class)).thenReturn(Mono.just(response));

        // when
        List<String> result = kakaoLocalApiServiceImpl.getLocationNames(37.4, 127.1).block();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("documents가 비어있으면 빈 리스트를 반환한다")
    void getLocationNames_withEmptyDocuments_returnsEmpty() {
        // given
        KakaoCoord2RegionResponseDto response = new KakaoCoord2RegionResponseDto(
                new KakaoCoord2RegionResponseDto.Meta(0),
                List.of()
        );
        when(responseSpec.bodyToMono(KakaoCoord2RegionResponseDto.class)).thenReturn(Mono.just(response));

        // when
        List<String> result = kakaoLocalApiServiceImpl.getLocationNames(37.4, 127.1).block();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("API 오류 발생 시 빈 리스트를 반환한다")
    void getLocationNames_onApiError_returnsEmpty() {
        // given
        when(responseSpec.bodyToMono(KakaoCoord2RegionResponseDto.class))
                .thenReturn(Mono.error(new BusinessException(
                        com.gitggal.clothesplz.exception.code.WeatherErrorCode.KAKAO_API_ERROR)));

        // when
        List<String> result = kakaoLocalApiServiceImpl.getLocationNames(37.4, 127.1).block();

        // then
        assertThat(result).isEmpty();
    }
}
