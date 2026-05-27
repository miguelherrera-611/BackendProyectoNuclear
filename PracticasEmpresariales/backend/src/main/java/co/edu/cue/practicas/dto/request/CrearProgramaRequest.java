package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CrearProgramaRequest {

    @NotBlank(message = "El nombre del programa es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La facultad es obligatoria")
    private Long facultadId;

    @Min(value = 1, message = "Debe tener al menos 1 práctica")
    private int numeroTotalPracticas = 1;

    private double promedioMinimoGeneral = 3.0;

    private List<RequisitoRequest> requisitos;

    @Data
    public static class RequisitoRequest {
        private int numeroPractica;
        private int creditosMinimos;
        private double promedioMinimo;
        private boolean requierePracticaAnteriorAprobada;
        private String documentosRequeridos;
    }
}
