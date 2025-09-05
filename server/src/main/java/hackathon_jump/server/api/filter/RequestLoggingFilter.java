package hackathon_jump.server.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("Incoming request: {} {} from {}",
                request.getMethod(), 
                request.getRequestURI(), 
                request.getRemoteAddr());

//      log.debug("Request headers: {}", getRequestHeaders(request));
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("Request completed: {} {} - Status: {}",
                    request.getMethod(), 
                    request.getRequestURI(), 
                    response.getStatus());
        }
    }
    
    private String getRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            headers.append(headerName).append(": ").append(request.getHeader(headerName)).append(", ");
        });
        return headers.toString();
    }
}
