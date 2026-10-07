package com.npcomputers.service;

import com.npcomputers.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimiter {
  private final Support s;

  public RateLimiter(Support s) {
    this.s = s;
  }

  @Transactional
  public void clear(String key) {
    s.db.update("delete from rate_limit where key=?", s.digest(key));
  }

  @Transactional(noRollbackFor = ApiException.class)
  public void hit(String key, int maximum, int seconds) {
    Integer n =
        s.db.queryForObject(
            "insert into rate_limit(key,attempts,window_end) values(?,1,now()+(? * interval '1"
                + " second')) on conflict(key) do update set attempts=case when"
                + " rate_limit.window_end<now() then 1 else rate_limit.attempts+1"
                + " end,window_end=case when rate_limit.window_end<now() then now()+(? * interval"
                + " '1 second') else rate_limit.window_end end returning attempts",
            Integer.class,
            s.digest(key),
            seconds,
            seconds);
    if (n > maximum)
      throw new ApiException(429, "THROTTLED", "Too many attempts. Try again later.");
  }
}
