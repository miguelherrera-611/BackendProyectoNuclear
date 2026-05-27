package co.edu.cue.practicas.config;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoCuenta;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra el primer Administrador DTI al arrancar el sistema.
 * Solo crea el usuario si no existe ninguno con ese correo,
 * por lo que es seguro ejecutarlo en cada arranque sin duplicar el usuario.
 *
 * El correo y la contraseña inicial se leen desde application.properties:
 *   app.init.admin.correo    → correo del primer DTI
 *   app.init.admin.password  → contraseña inicial (cámbiala antes de producción)
 *
 * Al arrancar, imprime en consola un aviso visual para que el desarrollador
 * sepa que el usuario fue creado y recuerde cambiar la contraseña.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // Acceso a la tabla de usuarios para verificar si el DTI inicial ya existe
    private final UsuarioRepository usuarioRepository;

    // Encripta la contraseña antes de guardarla; nunca se guarda en texto plano
    private final PasswordEncoder passwordEncoder;

    // Correo del administrador DTI inicial, configurable en application.properties
    @Value("${app.init.admin.correo}")
    private String correoDti;

    // Contraseña inicial del DTI, configurable en application.properties
    // IMPORTANTE: cambiar este valor antes de desplegar en producción
    @Value("${app.init.admin.password}")
    private String passwordDti;

    /**
     * Se ejecuta automáticamente al iniciar la aplicación (CommandLineRunner).
     * Crea el usuario DTI inicial solo si no existe ya en la base de datos.
     * En H2 (perfil de desarrollo) esto se ejecuta en cada arranque porque
     * la BD se borra y recrea; en PostgreSQL/MySQL solo se ejecuta la primera vez.
     */
    @Override
    public void run(String... args) {

        // Verificamos si ya existe un usuario con el correo configurado
        // Si existe, no hacemos nada (evitamos duplicados en BD persistentes como PostgreSQL)
        if (!usuarioRepository.existsByCorreo(correoDti)) {
            Usuario dti = Usuario.builder()
                    .nombre("Administrador DTI")
                    .correo(correoDti)
                    .passwordHash(passwordEncoder.encode(passwordDti))  // guardamos el hash BCrypt
                    .rol(Rol.ADMIN_DTI)
                    .activo(true)
                    .primerIngreso(false)           // el DTI inicial no necesita cambiar contraseña al entrar
                    .estadoCuenta(EstadoCuenta.ACTIVO)  // sembrado directamente, no requiere activación
                    .build();

            usuarioRepository.save(dti);

            // Aviso visible en consola para que el desarrollador sepa que el usuario fue creado
            log.info("=======================================================");
            log.info("  USUARIO DTI INICIAL CREADO");
            log.info("  Correo : {}", correoDti);
            log.info("  CAMBIA app.init.admin.password EN PRODUCCIÓN");
            log.info("=======================================================");
        }
    }
}
