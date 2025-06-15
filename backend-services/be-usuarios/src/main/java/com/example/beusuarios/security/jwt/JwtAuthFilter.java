package com.example.beusuarios.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User; // Spring Security's User
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList; // For creating UserDetails with no authorities

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtProvider jwtProvider;

    // We might need a UserDetailsService here if we want to load fresh UserDetails from DB.
    // For now, we'll construct UserDetails directly from JWT claims.
    // @Autowired
    // private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtProvider.validateToken(jwt)) {
                String username = jwtProvider.extractUsername(jwt); // Or email, depending on what's in subject
                Long userId = jwtProvider.extractUserId(jwt); // Assuming userId is in claims

                // Create UserDetails directly from JWT claims (no DB lookup here)
                // Adjust authorities as needed if roles/permissions are added to JWT
                UserDetails userDetails = new User(username, "", new ArrayList<>());
                // For UsernamePasswordAuthenticationToken, password field is not used if already authenticated by JWT.

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // We can also set a custom principal if needed, e.g., an object containing userId and email
                // Example: CustomPrincipal principal = new CustomPrincipal(userId, username);
                // UsernamePasswordAuthenticationToken authentication =
                //        new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());


                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Set authentication for user: {} with token", username);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        logger.trace("No JWT token found in request headers or does not start with Bearer");
        return null;
    }
}
