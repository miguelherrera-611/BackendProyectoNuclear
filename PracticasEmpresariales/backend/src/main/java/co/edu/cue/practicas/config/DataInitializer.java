package co.edu.cue.practicas.config;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra el primer Administrador DTI al arrancar el sistema.
 * Solo crea el usuario si no existe ninguno con ese correo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String correoDti = "dti@cue.edu.co";

        if (!usuarioRepository.existsByCorreo(correoDti)) {
            Usuario dti = Usuario.builder()
                    .nombre("Administrador DTI")
                    .correo(correoDti)
                    .passwordHash(passwordEncoder.encode("Admin2026!"))
                    .rol(Rol.ADMIN_DTI)
                    .activo(true)
                    .primerIngreso(false)
                    .build();

            usuarioRepository.save(dti);

            log.info("=======================================================");
            log.info("  USUARIO DTI INICIAL CREADO");
            log.info("  Correo  : {}", correoDti);
            log.info("  Password: Admin2026!");
            log.info("  CAMBIA ESTA CONTRASEÑA EN PRODUCCIÓN");
            log.info("=======================================================");
        }
    }
}
