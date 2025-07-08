package com.carbon.api.config;

import com.carbon.api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                        // 公開接口，不需要認證
                        .antMatchers("/api/vendor/GetToken").permitAll()
                        .antMatchers("/api/vendor/Register").permitAll()
                        .antMatchers("/api/vendor/public/StationInfo").permitAll()
                        // 測試接口，不需要認證
                        .antMatchers("/api/test/validate-token").permitAll()
                        .antMatchers("/api/test/generate-test-token").permitAll()
                        // 公開靜態資源
                        .antMatchers("/").permitAll()
                        .antMatchers("/login").permitAll()
                        .antMatchers("/login.html").permitAll()
                        .antMatchers("/register").permitAll()
                        .antMatchers("/register.html").permitAll()
                        .antMatchers("/common.js").permitAll()
                        .antMatchers("/stations.html").permitAll()
                        .antMatchers("/chargers.html").permitAll()
                        .antMatchers("/stationpower.html").permitAll()
                        .antMatchers("/stationmap.html").permitAll()
                        .antMatchers("/transactions.html").permitAll()
                        .antMatchers("/USER_GUIDE").permitAll()
                        .antMatchers("/USER_GUIDE.html").permitAll()
                        .antMatchers("/css/**").permitAll()
                        .antMatchers("/js/**").permitAll()
                        .antMatchers("/images/**").permitAll()
                        .antMatchers("/favicon.ico").permitAll()
                        .antMatchers("/*.js").permitAll()
                        .antMatchers("/*.css").permitAll()
                        // 需要認證且需要特定角色的接口
                        .antMatchers("/api/admin/**").hasRole("ADMIN")
                        // 所有其他接口需要認證
                        .antMatchers("/api/vendor/**").authenticated()
                        .anyRequest().authenticated()
                .and()
                .formLogin()
                        .loginPage("/login")
                        .defaultSuccessUrl("/transactions.html", true)
                        .permitAll()
                .and()
                .logout()
                        .logoutSuccessUrl("/login")
                        .permitAll()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 