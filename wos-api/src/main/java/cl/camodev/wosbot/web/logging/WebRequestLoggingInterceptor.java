package cl.camodev.wosbot.web.logging;

import cl.camodev.wosbot.logging.WebLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that records basic information about HTTP interactions to a dedicated web log.
 */
@Component
public class WebRequestLoggingInterceptor implements HandlerInterceptor {

    private static final String ATTRIBUTE_START_TIME = "webLoggerStartTime";

    private final WebLogger webLogger = new WebLogger(WebRequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTRIBUTE_START_TIME, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        long durationMs = calculateDuration(request);
        String message = formatMessage(request, response, durationMs);

        if (ex != null) {
            webLogger.error(message, ex);
        } else if (response.getStatus() >= 400) {
            webLogger.warn(message);
        } else {
            webLogger.info(message);
        }
    }

    private long calculateDuration(HttpServletRequest request) {
        Object startAttribute = request.getAttribute(ATTRIBUTE_START_TIME);
        if (startAttribute instanceof Long startTime) {
            long elapsed = System.nanoTime() - startTime;
            return Math.max(0L, elapsed / 1_000_000);
        }
        return 0L;
    }

    private String formatMessage(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = (query == null || query.isBlank()) ? uri : uri + "?" + query;
        return method + " " + fullPath + " -> " + response.getStatus() + " (" + durationMs + " ms)";
    }
}
