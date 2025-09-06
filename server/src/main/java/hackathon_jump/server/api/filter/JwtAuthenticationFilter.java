package hackathon_jump.server.api.filter;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.model.dto.Session;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;
//    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("JWT Authentication filter processing request: {}", request.getRequestURI());

        String requestUri = request.getRequestURI();
        if(requestUri.startsWith("/auth") ||
            requestUri.startsWith("/login") ||
            requestUri.startsWith("/api/public")) {
            filterChain.doFilter(request, response);
            return;
        }

        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.info("Invalid JWT token!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid Authorization header\"}");
            response.setContentType("application/json");
            return;
        }
        
        jwt = authHeader.substring(7);
        try {
            Session session = jwtService.validateAndGetSession(jwt);
            logger.debug("translated jwt into session: " + session);
            request.setAttribute("session", session);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(session.getGoogleEmailAddresses().getFirst(),
                            null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);

//            String userEmail = jwtService.validateAndGetSubject(jwt);
//            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
//                if (!jwtService.isTokenValid(jwt, userDetails)) {
//                    throw new JwtException(jwt);
//                }
//                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.getAuthorities()
//                );
//                authToken.setDetails(
//                        new WebAuthenticationDetailsSource().buildDetails(request)
//                );
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
        } catch (Exception e) {
            // Token is invalid, send 401 response
            logger.debug("Invalid JWT token: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid JWT token\"}");
            response.setContentType("application/json");
            return;
        }
    }
}
