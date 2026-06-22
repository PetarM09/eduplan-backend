package rs.skola.platforma.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.skola.platforma.auth.security.JwtAuthenticationFilter;
import rs.skola.platforma.auth.security.JwtProperties;
import rs.skola.platforma.common.web.ApiResponse;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

        private static final String[] PUBLIC_RUTE = {
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/pozivnice/info/*",
                        "/api/v1/pozivnice/aktiviraj/*",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
        };

        private final JwtAuthenticationFilter jwtFilter;
        private final ObjectMapper objectMapper;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
                provider.setPasswordEncoder(encoder);
                return new ProviderManager(provider);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOriginPatterns(List.of(
                                "http://localhost:5173",
                                "https://eduplan-frontend-plum.vercel.app",
                                "https://behindclasses.com",
                                "https://www.behindclasses.com"));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("*"));
                cfg.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
                cfg.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_RUTE).permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(eh -> eh
                                                .authenticationEntryPoint(
                                                                (req, res, ex) -> writeErrorResponse(res,
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "NEAUTORIZOVANO",
                                                                                "Token je nevalidan ili nedostaje"))
                                                .accessDeniedHandler((req, res, ex) -> writeErrorResponse(res,
                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                "PRISTUP_ZABRANJEN", "Nemate dozvolu za ovu akciju")))
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        private void writeErrorResponse(HttpServletResponse res, int status, String code, String message)
                        throws java.io.IOException {
                res.setStatus(status);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                res.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(res.getWriter(), ApiResponse.error(code, message));
        }
}
