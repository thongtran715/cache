package com.cach;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CacheStats {

  private long hitCount = 0;
  private long missCount = 0;
  private long evictionCount = 0;

  public void incrementHit() {
    this.hitCount++;
  }

  public void incrementMiss() {
    this.missCount++;
  }

  public void incrementEviction() {
    this.evictionCount++;
  }

  public long sum(long a, long b) {
    long naiveSum = a + b;
    if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
      return naiveSum;
    }
    return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
  }

  public double hitRate() {
    long sum = sum(hitCount, missCount);
    if (sum == 0) {
      return 1.0;
    }
    return (double) hitCount / sum;
  }
}
