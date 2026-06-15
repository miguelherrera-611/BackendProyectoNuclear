package co.edu.cue.practicas.repository.usuario;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCorreoAndActivoTrue(String correo);

    boolean existsByCorreo(String correo);

    Page<Usuario> findByActivoTrue(Pageable pageable);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    Page<Usuario> findByRolAndActivoTrue(Rol rol, Pageable pageable);

    /** OCL: minimoUnDTIActivo — verifica cuántos DTI activos hay */
    long countByRolAndActivoTrue(Rol rol);

    /** Estudiantes por estado y programa (scope del Coordinador de Prácticas) */
    Page<Usuario> findByRolAndEstadoEstudianteAndPrograma_Id(
            Rol rol, EstadoEstudiante estadoEstudiante, Long programaId, Pageable pageable);

    /** Estudiantes por estado y facultad (scope de Coordinación Académica) */
    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol AND u.estadoEstudiante = :estado AND u.programa.facultad.id = :facultadId")
    Page<Usuario> findEstudiantesPorEstadoYFacultad(
            @Param("rol") Rol rol,
            @Param("estado") EstadoEstudiante estado,
            @Param("facultadId") Long facultadId,
            Pageable pageable);

    /** GPE-147 — todos los estudiantes de una facultad sin importar estado (scope Coordinación Académica) */
    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol AND u.programa.facultad.id = :facultadId AND u.activo = true")
    Page<Usuario> findEstudiantesPorFacultad(
            @Param("rol") Rol rol,
            @Param("facultadId") Long facultadId,
            Pageable pageable);

    /** GPE-147 — Coordinador de Prácticas: solo APTOS enviados al proceso de su programa */
    Page<Usuario> findByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndPrograma_IdAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante, Long programaId, Pageable pageable);

    long countByRolAndEstadoEstudianteAndActivoTrue(Rol rol, EstadoEstudiante estadoEstudiante);

    long countByRolAndEstadoEstudianteAndEnviadoAlProcesoFalseAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante);

    long countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante);

    @Query("""
            SELECT COUNT(u)
            FROM Usuario u
            WHERE u.rol = :rol
              AND u.estadoEstudiante = :estado
              AND u.enviadoAlProceso = true
              AND u.activo = true
              AND u.programa.id = :programaId
              AND NOT EXISTS (
                  SELECT i
                  FROM InstanciaPractica i
                  WHERE i.expediente.estudiante = u
                    AND i.estado NOT IN :estadosFinales
              )
            """)
    long countEstudiantesAptosDisponibles(
            @Param("rol") Rol rol,
            @Param("estado") EstadoEstudiante estado,
            @Param("programaId") Long programaId,
            @Param("estadosFinales") java.util.List<EstadoPractica> estadosFinales);

    boolean existsByIdentificacion(String identificacion);

    Page<Usuario> findByFacultad_IdAndActivoTrue(Long facultadId, Pageable pageable);

    List<Usuario> findByRolAndFacultad_IdAndActivoTrue(Rol rol, Long facultadId);

    List<Usuario> findByRolAndPrograma_IdAndActivoTrue(Rol rol, Long programaId);

    long countByRolAndEstadoEstudianteAndPrograma_IdAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante, Long programaId);

    long countByRolAndEstadoEstudianteAndPrograma_Facultad_IdAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante, Long facultadId);

    long countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndPrograma_Facultad_IdAndActivoTrue(
            Rol rol, EstadoEstudiante estadoEstudiante, Long facultadId);
}
