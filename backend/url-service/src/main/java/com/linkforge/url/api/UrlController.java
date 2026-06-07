package com.linkforge.url.api;

import com.linkforge.common.ratelimit.RedisTokenBucketRateLimiter;
import com.linkforge.url.api.UrlDtos.CreateUrlRequest;
import com.linkforge.url.api.UrlDtos.UrlResponse;
import com.linkforge.url.domain.ShortUrl;
import com.linkforge.url.service.UrlCreationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
  private final UrlCreationService service;
  private final RedisTokenBucketRateLimiter rateLimiter;

  public UrlController(UrlCreationService service, RedisTokenBucketRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping
  public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request, HttpServletRequest servletRequest) {
    if (!rateLimiter.allow("url-create:" + clientIp(servletRequest))) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    var result = service.create(request);
    HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(toResponse(result.shortUrl()));
  }

  @GetMapping
  public List<UrlResponse> list(@RequestParam(required = false) String ownerId) {
    return service.list(ownerId).stream().map(this::toResponse).toList();
  }

  private UrlResponse toResponse(ShortUrl shortUrl) {
    return new UrlResponse(
        shortUrl.getId(),
        shortUrl.getShortCode(),
        service.shortUrl(shortUrl.getShortCode()),
        shortUrl.getOriginalUrl(),
        shortUrl.getExpiresAt(),
        shortUrl.getCreatedAt());
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
