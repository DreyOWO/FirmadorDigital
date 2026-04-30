package cr.libre.firmador.backend.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private UUID id;
    private String role;
    
    public UserPrincipal(UUID id) {
        this(id, "user");
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String normalizedRole = role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
    }
    
    @Override
    public String getPassword() {
        return null;
    }
    
    @Override
    public String getUsername() {
        return id.toString();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}

// Made with Bob
