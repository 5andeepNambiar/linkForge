package com.linkforge.redirect.service;

import com.linkforge.common.events.ClickEvent.DeviceMetadata;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UserAgentClassifier {
  public DeviceMetadata classify(String userAgent) {
    String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
    String type = ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") ? "mobile" : "desktop";
    String browser = ua.contains("edg") ? "Edge" : ua.contains("chrome") ? "Chrome" : ua.contains("firefox") ? "Firefox" : ua.contains("safari") ? "Safari" : "Other";
    String os = ua.contains("windows") ? "Windows" : ua.contains("mac os") ? "macOS" : ua.contains("android") ? "Android" : ua.contains("iphone") ? "iOS" : ua.contains("linux") ? "Linux" : "Other";
    return new DeviceMetadata(type, browser, os);
  }
}
