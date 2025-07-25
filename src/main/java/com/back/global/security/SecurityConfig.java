package com.back.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/favicon.ico").permitAll()
                                .requestMatchers("/h2-console/**").permitAll()

                                // 모두 접근 가능한 API
                                .requestMatchers(HttpMethod.GET,  "/", "/main", "/news", "/news/{id}", "/today-news", "/quiz/ox").permitAll()
                                .requestMatchers(HttpMethod.POST, "api/members/login", "api/members/join", "api/members/logout").permitAll()

                                // 회원만 접근 가능한 API
                                .requestMatchers(HttpMethod.GET,  "/quiz/detail/**", "/today-quiz", "/quiz/ox/detail/**", "/mypage").authenticated()
                                .requestMatchers(HttpMethod.POST, "/quiz/detail/**/submit", "/today-quiz/submit", "/quiz/ox/detail/**/submit").authenticated()
                                .requestMatchers(HttpMethod.PUT,  "/mypage").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/mypage").authenticated()

                                // 관리자만 접근 가능한 API
                                .requestMatchers(HttpMethod.GET, "/admin").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/admin/news/**").hasRole("ADMIN")

                                // 그 외는 모두 인증 필요
                                .requestMatchers("/api/*/**").authenticated()

                                // 그 외는 모두 허용
                                .anyRequest().permitAll()
                )
                .headers(
                        headers -> headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                )
                ).csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//                .exceptionHandling(
//                        exceptionHandling -> exceptionHandling
//                                .authenticationEntryPoint(
//                                        (request, response, authException) -> {
//                                            response.setContentType("application/json;charset=UTF-8");
//
//                                            response.setStatus(401);
//                                            response.getWriter().write(
//                                                    Ut.json.toString(
//                                                            new RsData<Void>(
//                                                                    "401-1",
//                                                                    "로그인 후 이용해주세요."
//                                                            )
//                                                    )
//                                            );
//                                        }
//                                )
//                                .accessDeniedHandler(
//                                        (request, response, accessDeniedException) -> {
//                                            response.setContentType("application/json;charset=UTF-8");
//
//                                            response.setStatus(403);
//                                            response.getWriter().write(
//                                                    Ut.json.toString(
//                                                            new RsData<Void>(
//                                                                    "403-1",
//                                                                    "권한이 없습니다."
//                                                            )
//                                                    )
//                                            );
//                                        }
//                                )
//                );

        return http.build();
    }



    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOrigins(List.of("https://cdpn.io", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true);

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(List.of("*"));

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/grid/**", configuration);
        return source;
    }
}
