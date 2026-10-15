package za.co.rmb.approvalforum.docgen.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import za.co.rmb.approvalforum.docgen.security.JwtAuthorizationFilter;

/**
 * The legacy docgen security configuration on Spring Security 7.
 *
 * <p>Legacy extended WebSecurityConfigurerAdapter, removed in Spring Security 6. The rules are the same: CORS with
 * the permit-default values, no CSRF, the same public paths, everything else authenticated by the legacy
 * {@link JwtAuthorizationFilter} (added where Spring placed it, at BasicAuthenticationFilter's position), stateless,
 * and the same in-memory user behind the filter's AuthenticationManager. {@code @EnableGlobalMethodSecurity(securedEnabled
 * = true)} became {@code @EnableMethodSecurity(securedEnabled = true)}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Value("${authentication.jwt.secret}")
    private String jwtSecret;

    private static final String[] AUTH_WHITELIST = {
            // -- swagger ui
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // other public endpoints of your API may be appended to this array
            "/ping",
            "/actuator/**"
    };

    @Bean
    SecurityFilterChain docgenSecurity(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Spring Security 5 did not authorise the container's error dispatch; 6+ does, which would
                        // turn every error answer into an empty 403 instead of Boot's JSON error body
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated())
                // Spring Security 5's default Http403ForbiddenEntryPoint: sendError(403, "Access Denied")
                .exceptionHandling(handling -> handling.authenticationEntryPoint(
                        (request, response, e) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied")))
                .addFilterAt(new JwtAuthorizationFilter(authenticationManager, this.jwtSecret), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .authorities("ROLE_USER")
                .build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }
}
