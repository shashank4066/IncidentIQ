package dev.incidentiq.api;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean UserDetailsService users(@Value("${app.auth.admin-password}") String admin,@Value("${app.auth.viewer-password}") String viewer,@Value("${app.auth.worker-password}") String worker) {
  var encoder=new BCryptPasswordEncoder();
  return new InMemoryUserDetailsManager(User.withUsername("admin").password(encoder.encode(admin)).roles("ADMIN","RESPONDER","VIEWER").build(),User.withUsername("viewer").password(encoder.encode(viewer)).roles("VIEWER").build(),User.withUsername("rca-worker").password(encoder.encode(worker)).roles("WORKER").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c->c.disable())
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**","/actuator/info","/error").permitAll()
    .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
    .requestMatchers("/api/internal/**").hasRole("WORKER")
    .requestMatchers(HttpMethod.GET,"/api/**").hasRole("VIEWER")
    .requestMatchers(HttpMethod.POST,"/api/services").hasRole("ADMIN")
    .requestMatchers("/api/**").hasRole("RESPONDER").anyRequest().denyAll())
   .httpBasic(b->b.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/problem+json");res.getWriter().write("{\"title\":\"Authentication required\",\"status\":401}");}))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
   .build();
 }
}
