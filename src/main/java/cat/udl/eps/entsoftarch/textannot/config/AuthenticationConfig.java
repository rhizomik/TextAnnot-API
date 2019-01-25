package cat.udl.eps.entsoftarch.textannot.config;

import cat.udl.eps.entsoftarch.textannot.domain.Admin;
import cat.udl.eps.entsoftarch.textannot.domain.Linguist;
import cat.udl.eps.entsoftarch.textannot.domain.User;
import cat.udl.eps.entsoftarch.textannot.repository.AdminRepository;
import cat.udl.eps.entsoftarch.textannot.repository.LinguistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

@Configuration
public class AuthenticationConfig extends GlobalAuthenticationConfigurerAdapter {

  @Value("${default-password}")
  String defaultPassword;

  @Autowired BasicUserDetailsService basicUserDetailsService;
  @Autowired AdminRepository adminRepository;
  @Autowired LinguistRepository linguistRepository;

  @Override
  public void init(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .userDetailsService(basicUserDetailsService)
        .passwordEncoder(User.passwordEncoder);

    if (!adminRepository.existsById("admin")) {
      Admin admin = new Admin();
      admin.setEmail("admin@textannot.org");
      admin.setUsername("admin");
      admin.setPassword(defaultPassword);
      admin.encodePassword();
      adminRepository.save(admin);
    }
    if (!linguistRepository.existsById("user")) {
      Linguist user = new Linguist();
      user.setEmail("user@textannot.org");
      user.setUsername("user");
      user.setPassword(defaultPassword);
      user.encodePassword();
      linguistRepository.save(user);
    }
  }
}
