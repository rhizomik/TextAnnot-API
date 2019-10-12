package cat.udl.eps.entsoftarch.textannot.domain;

import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Admin extends User {

    @Override
    @Transient
    /**
     * This function returns collection of admin credentials.
     * Returns the collection of granted authority for the user.
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN, ROLE_LINGUIST");
    }
}
