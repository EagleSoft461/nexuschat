package com.nexuschat.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.message.capacity:30}")
    private int messageCapacity;

    @Value("${rate-limit.message.refill-per-minute:30}")
    private int messageRefill;

    @Value("${rate-limit.auth.capacity:10}")
    private int authCapacity;

    @Value("${rate-limit.auth.refill-per-minute:10}")
    private int authRefill;

    @Autowired
    private ProxyManager<String> bucketProxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit API endpoints
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, path);
        boolean isAuth = path.startsWith("/api/auth/");
        int capacity = isAuth ? authCapacity : messageCapacity;
        int refill   = isAuth ? authRefill   : messageRefill;

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(refill, Duration.ofMinutes(1))
                        .build())
                .build();

        Bucket bucket = bucketProxyManager.builder().build(key, configSupplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit exceeded. Retry after " + waitSeconds + " seconds.\"}");
        }
    }

    private String resolveKey(HttpServletRequest request, String path) {
        // Authenticated users → rate limit by username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "rl:user:" + auth.getName() + ":" + (path.startsWith("/api/auth/") ? "auth" : "api");
        }
        // Unauthenticated → rate limit by IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return "rl:ip:" + ip + ":" + (path.startsWith("/api/auth/") ? "auth" : "api");
    }
}
