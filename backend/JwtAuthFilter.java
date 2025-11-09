package com.hungerexpress.security;

import com.hungerexpress.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    
    @Value("${app.jwt.secret}")
    private String secret;

    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip JWT extraction for auth endpoints (to avoid circular dependencies)
        if (path != null && path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip JWT for static resources (no need to process)
        if (path != null && (path.startsWith("/uploads/") || path.startsWith("/api/media/"))) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // NOTE: Order endpoints are public (allow guest orders)
        // But we should still process JWT if present to link orders to logged-in users
        // So we DON'T skip JWT processing here - we continue to extract token below
        
        // Skip JWT for cart endpoints (allow guest carts)
        if (path != null && path.startsWith("/api/cart")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip JWT for payment webhooks
        if (path != null && path.equals("/api/payments/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Try to get token from Authorization header first
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        } else {
            // For SSE connections, token might be in query parameter
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                token = queryToken;
                System.out.println("[JwtAuthFilter] Using token from query parameter for SSE");
            }
        }
        
        if (token != null) {
            try {
                System.out.println("[JwtAuthFilter] Processing JWT token for: " + path);
                String email = jwt.extractUsername(token);
                Key signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
                Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build().parseClaimsJws(token).getBody();
                String role = claims.get("role", String.class);
                if (role == null || role.isBlank()) role = "CUSTOMER";
                String authority = role.startsWith("ROLE_") ? role : ("ROLE_" + role);
                var authToken = new UsernamePasswordAuthenticationToken(email, null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("[JwtAuthFilter] ✅ Authenticated user: " + email + " with role: " + authority);
            } catch (Exception e){
                // Log the error for debugging
                System.err.println("[JwtAuthFilter] ❌ Failed to parse token: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[JwtAuthFilter] ⚠️ No JWT token found for: " + path);
        }
        filterChain.doFilter(request, response);
    }
}

