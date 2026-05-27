package co.edu.cue.practicas.event;

import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER — Observador concreto: inactivación de tutores.
 *
 * SOLID — SRP: responsabilidad única → inactivar tutores cuando
 *              la empresa se inactiva. Nada más.
 * SOLID — OCP: implementa EmpresaObserver sin modificar nada existente.
 * SOLID — DIP: EmpresaService inyecta List<EmpresaObserver>, no esta clase.
 *
 * Regla GPE-151: "Al inactivar empresa → tutores se inactivan automáticamente."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TutorInactivacionObserver implements EmpresaObserver {

    private final TutorEmpresarialRepository tutorRepository;

    @Override
    public void onEmpresaEvento(Long empresaId, String evento) {
        if ("EMPRESA_INACTIVADA".equals(evento)) {
            log.info("[Observer] Empresa {} inactivada → inactivando tutores", empresaId);
            tutorRepository.findByEmpresaIdAndActivoTrue(empresaId)
                    .forEach(tutor -> {
                        tutor.desactivar();
                        tutorRepository.save(tutor);
                    });
        }
    }
}
