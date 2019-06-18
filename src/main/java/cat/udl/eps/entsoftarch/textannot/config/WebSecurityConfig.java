package cat.udl.eps.entsoftarch.textannot.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
  @Value("${allowed-origins}")
  String[] allowedOrigins;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.GET, "/admins*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.POST, "/admins*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.PUT, "/admins*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.PATCH, "/admins*/*").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/admins*/**").hasRole("ADMIN")

        .antMatchers(HttpMethod.POST, "/linguists*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/linguists*/**").hasRole("ADMIN")

        .antMatchers(HttpMethod.GET, "/identity").authenticated()

        .antMatchers(HttpMethod.POST, "/tags*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/tags*/**").hasRole("ADMIN")

        .antMatchers(HttpMethod.POST, "/projects*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/projects*/**").hasRole("ADMIN")

        .antMatchers(HttpMethod.POST, "/metadataFields*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.PUT, "/metadataFields*/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.PATCH, "/metadataFields*/*").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/metadataFields*/**").hasRole("ADMIN")

        .antMatchers(HttpMethod.POST, "/**/*").authenticated()
        .antMatchers(HttpMethod.PUT, "/**/*").authenticated()
        .antMatchers(HttpMethod.PATCH, "/**/*").authenticated()
        .antMatchers(HttpMethod.DELETE, "/**/*").authenticated()
        .anyRequest().permitAll()
        .and()
        .httpBasic().realmName("TextAnnot")
        .and()
        .cors()
        .and()
        .csrf().disable()
        .headers().frameOptions().sameOrigin();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOrigins(Arrays.asList(allowedOrigins));
    corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
    corsConfiguration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfiguration);
    return source;
  }
}
