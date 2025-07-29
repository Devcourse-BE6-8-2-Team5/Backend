package com.back.global.security;

import com.back.global.rsData.RsData;
import com.back.global.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    private final CustomAuthenticationFilter customAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/favicon.ico").permitAll()
                                .requestMatchers("/h2-console/**").permitAll()

                                //모두 접근 가능한 API
                                .requestMatchers(HttpMethod.GET,  "/메인페이지/뉴스목록", "/뉴스상세페이지", "/오늘의뉴스페이지", "/ox퀴즈페이지").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/members/login", "/api/members/join").permitAll()
                                .requestMatchers(HttpMethod.DELETE, "/api/members/logout").permitAll()

                                // 회원만 접근 가능한 API
                                .requestMatchers(HttpMethod.GET,  "/상세퀴즈페이지", "/오늘의퀴즈페이지", "/ox퀴즈상세페이지", "/api/members/info").authenticated()
                                .requestMatchers(HttpMethod.POST, "/상세퀴즈제출", "/오늘의퀴즈제출", "/ox퀴즈제출").authenticated()
                                .requestMatchers(HttpMethod.PUT,  "/api/members/info").authenticated() // 내정보 수정 api로 변경해야함.
                                .requestMatchers(HttpMethod.DELETE, "/마이페이지회원탈퇴").authenticated()

                                // 관리자만 접근 가능한 API
                                .requestMatchers(HttpMethod.GET, "/관리자페이지").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/관리자페이지뉴스삭제").hasRole("ADMIN")

                                // 그 외는 모두 인증 필요
//                                .requestMatchers("/api/*/**").authenticated()
                                .requestMatchers("/api/*/**").permitAll()


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
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    401,
                                                                    "로그인 후 이용해주세요.",
                                                                    null
                                                            )
                                                    )
                                            );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    403,
                                                                    "권한이 없습니다.",
                                                                    null
                                                            )
                                                    )
                                            );
                                        }
                                )
                );
        return http.build();
    }


    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true); // 쿠키 허용

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/admin/**", configuration);
        return source;
    }
}
