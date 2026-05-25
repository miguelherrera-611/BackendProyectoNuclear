package co.edu.cue.practicas.security;

import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreoAndActivoTrue(correo)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado o inactivo: " + correo));
    }
}
