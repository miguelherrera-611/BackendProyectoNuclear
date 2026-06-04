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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin.correo}")
    private String correoDti;

    @Value("${app.init.admin.password}")
    private String passwordDti;

    @Value("${app.init.crear.coordinador.test:false}")
    private boolean crearCoordinadorTest;

    @Value("${app.init.coordinador.test.correo:coord.test@cue.edu.co}")
    private String correoCoordinadorTest;

    @Value("${app.init.coordinador.test.password}")
    private String passwordCoordinadorTest;

    @Override
    public void run(String... args) {

        // Crear DTI inicial
        if (!usuarioRepository.existsByCorreo(correoDti)) {
            Usuario dti = Usuario.builder()
                    .nombre("Administrador DTI")
                    .correo(correoDti)
                    .passwordHash(passwordEncoder.encode(passwordDti))
                    .rol(Rol.ADMIN_DTI)
                    .activo(true)
                    .primerIngreso(false)
                    .estadoCuenta(EstadoCuenta.ACTIVO)
                    .build();

            usuarioRepository.save(dti);

            log.info("=======================================================");
            log.info("  USUARIO DTI INICIAL CREADO");
            log.info("  Correo : {}", correoDti);
            log.info("  CAMBIA app.init.admin.password EN PRODUCCION");
            log.info("=======================================================");
        }

        // Crear coordinador de practicas para tests de integracion
        if (crearCoordinadorTest && !usuarioRepository.existsByCorreo(correoCoordinadorTest)) {
            Usuario coord = Usuario.builder()
                    .nombre("Coordinador Test")
                    .correo(correoCoordinadorTest)
                    .passwordHash(passwordEncoder.encode(passwordCoordinadorTest))
                    .rol(Rol.COORDINADOR_PRACTICAS)
                    .primerIngreso(false)
                    .activo(true)
                    .estadoCuenta(EstadoCuenta.ACTIVO)
                    .build();

            usuarioRepository.save(coord);

            log.info("  COORDINADOR TEST CREADO: {}", correoCoordinadorTest);
        }
    }
}