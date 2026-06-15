package co.edu.cue.practicas.service.reporte;

import co.edu.cue.practicas.dto.response.TableroGerencialResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class TableroGerencialDireccionService {

    private final ProgramaRepository programaRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final NotaFinalCoordinadorRepository notaRepository;
    private final EmpresaRepository empresaRepository;

    public TableroGerencialResponse consultar(CustomUserDetails actor) {
        if (actor.getRol() != Rol.DIRECCION) {
            throw new AccesoNoAutorizadoException("El tablero gerencial es exclusivo para Direccion.");
        }
        // SPRINT 4 - Facade: consolida practicas, notas, programas y empresas en una vista gerencial.
        var porPrograma = new LinkedHashMap<String, Long>();
        programaRepository.findAll().forEach(programa -> porPrograma.put(programa.getNombre(),
                instanciaRepository.countByEstadoAndExpediente_Estudiante_Programa_Id(EstadoPractica.EN_CURSO, programa.getId())));
        long aprobados = notaRepository.countByResultado(ResultadoPractica.APROBADO);
        long noAprobados = notaRepository.countByResultado(ResultadoPractica.NO_APROBADO);
        long total = aprobados + noAprobados;
        double tasa = total == 0 ? 0.0 : Math.round((aprobados * 10000.0 / total)) / 100.0;
        // SPRINT 4 - Builder: arma el tablero solo con indicadores agregados, sin datos individuales.
        return TableroGerencialResponse.builder()
                .practicantesEnCursoPorPrograma(porPrograma)
                .tasaAprobacionGlobal(tasa)
                .empresasActivas(empresaRepository.countByEstado(EstadoEmpresa.ACTIVA))
                .build();
    }
}
