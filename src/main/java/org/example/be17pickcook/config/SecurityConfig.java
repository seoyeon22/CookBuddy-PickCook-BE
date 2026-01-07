package org.example.be17pickcook.config;

import org.example.be17pickcook.config.filter.JwtAuthFilter;
import org.example.be17pickcook.config.filter.LoginFilter;
import lombok.RequiredArgsConstructor;
import org.example.be17pickcook.config.oauth.OAuth2AuthenticationSuccessHandler;
import org.example.be17pickcook.domain.user.mapper.UserMapper;
import org.example.be17pickcook.domain.user.service.OAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final AuthenticationConfiguration authConfiguration;  // 변수명 변경
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final UserMapper userMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();  // 변수명 변경

        corsConfiguration.addAllowedOrigin("https://pick-cook.kro.kr");
        corsConfiguration.setAllowedOrigins(
                List.of("https://pick-cook.kro.kr:8443")
        );
        corsConfiguration.addAllowedOriginPattern("http://localhost:*");
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);  // 올바른 변수 사용
        return source;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(
                (auth) -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/user/signup",
                                "/api/user/verify",
                                "/api/user/check-email",
                                "/api/user/find-email",
                                "/api/user/request-password-reset",
                                "/api/user/reset-password",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/api/user/addresses/**").authenticated()
                        .requestMatchers("/test/*").hasRole("USER")
                        .anyRequest().permitAll()
        );

        http.oauth2Login(config -> {
                    config.userInfoEndpoint(
                            endpoint ->
                                    endpoint.userService(oAuth2UserService)
                    );
                    config.successHandler(oAuth2AuthenticationSuccessHandler);
                }
        );

        http.cors(cors ->
                cors.configurationSource(corsConfigurationSource()));

        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);

        http.addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        LoginFilter loginFilter = new LoginFilter(authConfiguration.getAuthenticationManager(), userMapper);  // 변경된 변수명 사용
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
