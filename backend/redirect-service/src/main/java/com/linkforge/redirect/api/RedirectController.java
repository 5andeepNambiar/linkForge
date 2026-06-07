package com.linkforge.redirect.api;

import com.linkforge.common.ratelimit.RedisTokenBucketRateLimiter;
import com.linkforge.redirect.service.ClickEventPublisher;
import com.linkforge.redirect.service.RedirectResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {
  private final RedirectResolver resolver;
  private final ClickEventPublisher clickEventPublisher;
  private final RedisTokenBucketRateLimiter rateLimiter;

  public RedirectController(RedirectResolver resolver, ClickEventPublisher clickEventPublisher, RedisTokenBucketRateLimiter rateLimiter) {
    this.resolver = resolver;
    this.clickEventPublisher = clickEventPublisher;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/{shortCode:[A-Za-z0-9]+}")
  public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
    String ip = clientIp(request);
    if (!rateLimiter.allow("redirect:" + ip)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    return resolver.resolve(shortCode)
        .map(entry -> {
          clickEventPublisher.publish(entry, ip, request.getHeader(HttpHeaders.USER_AGENT));
          return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(entry.originalUrl())).<Void>build();
        })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
