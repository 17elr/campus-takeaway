package cn.campus.takeaway.config;

import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(AppMapper mapper){ return username -> { var user=mapper.findUser(username); if(user==null) throw new UsernameNotFoundException(username); return user; }; }
    @Bean SecurityFilterChain security(HttpSecurity http, AppMapper mapper) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/","/login","/register","/error","/css/**","/js/**","/images/**").permitAll()
                .requestMatchers("/student/**").hasRole("STUDENT").requestMatchers("/merchant/**").hasRole("MERCHANT")
                .requestMatchers("/rider/**").hasRole("RIDER").requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/home",true).permitAll())
            .logout(l -> l.logoutSuccessUrl("/").permitAll())
            .addFilterAfter(new ActiveAccountFilter(mapper),org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
