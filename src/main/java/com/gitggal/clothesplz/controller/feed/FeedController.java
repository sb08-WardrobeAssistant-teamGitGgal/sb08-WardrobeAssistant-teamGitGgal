package com.gitggal.clothesplz.controller.feed;

import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.service.feed.FeedService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/feeds")
public class FeedController {

  private final FeedService feedService;

  @PostMapping
  public ResponseEntity<FeedDto> create(
      @Valid @RequestBody FeedCreateRequest feedCreateRequest
  ) {
    log.info("[Controller] 피드 생성 요청 시작");
    FeedDto feedDto = feedService.createFeed(feedCreateRequest);

    log.info("[Controller] 피드 생성 요청 완료");
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(feedDto);
  }

  @PatchMapping("/{feedId}")
  public ResponseEntity<FeedDto> update(
      @PathVariable UUID feedId,
      @Valid @RequestBody FeedUpdateRequest feedUpdateRequest
  ) {
    log.info("[Controller] 피드 수정 요청 시작");
    FeedDto feedDto = feedService.updateFeed(feedId, feedUpdateRequest);

    log.info("[Controller] 피드 수정 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(feedDto);
  }
}
