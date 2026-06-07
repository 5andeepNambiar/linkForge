package com.linkforge.common.ids;

public final class Base62 {
  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

  private Base62() {
  }

  public static String encode(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("value must be non-negative");
    }
    if (value == 0) {
      return "0";
    }
    StringBuilder builder = new StringBuilder();
    long current = value;
    while (current > 0) {
      builder.append(ALPHABET[(int) (current % 62)]);
      current /= 62;
    }
    return builder.reverse().toString();
  }
}
