package co.edu.cue.practicas.security;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad Usuario y Spring Security UserDetails.
 * Expone rol, etiqueta de cargo y scope (facultad/programa) para el Proxy.
 */
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getId() { return usuario.getId(); }
    public String getNombre() { return usuario.getNombre(); }
    public Rol getRol() { return usuario.getRol(); }
    public EtiquetaCargo getEtiquetaCargo() { return usuario.getEtiquetaCargo(); }
    public Long getFacultadId() {
        return usuario.getFacultad() != null ? usuario.getFacultad().getId() : null;
    }
    public Long getProgramaId() {
        return usuario.getPrograma() != null ? usuario.getPrograma().getId() : null;
    }
    public Usuario getUsuario() { return usuario; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override public String getPassword() { return usuario.getPasswordHash(); }
    @Override public String getUsername() { return usuario.getCorreo(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return usuario.isActivo(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return usuario.isActivo(); }
}
