package za.co.rmb.approvalforum.docgen.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private String jwtSecret;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, String jwtSecret) {
        super(authenticationManager);
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        if (authentication == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotEmpty(token) && token.startsWith("Bearer ") && StringUtils.isNotEmpty(this.jwtSecret)) {
            try {
                byte[] signingKey = Base64.getDecoder().decode(this.jwtSecret);

                Jws<Claims> parsedToken = Jwts.parser()
                    .setSigningKey(signingKey)
                    .parseClaimsJws(token.replaceAll("Bearer ", ""));

                String username = parsedToken
                    .getBody()
                    .getSubject();

                if (StringUtils.isNotEmpty(username)) {
                    return new UsernamePasswordAuthenticationToken(username, null, null);
                }
            } catch (ExpiredJwtException exception) {
                log.warn("Request to parse expired JWT : {} failed : {}", token, exception.getMessage());
            } catch (UnsupportedJwtException exception) {
                log.warn("Request to parse unsupported JWT : {} failed : {}", token, exception.getMessage());
            } catch (MalformedJwtException exception) {
                log.warn("Request to parse invalid JWT : {} failed : {}", token, exception.getMessage());
            } catch (SignatureException exception) {
                log.warn("Request to parse JWT with invalid signature : {} failed : {}", token, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                log.warn("Request to parse empty or null JWT : {} failed : {}", token, exception.getMessage());
            }
        }

        return null;
    }
}