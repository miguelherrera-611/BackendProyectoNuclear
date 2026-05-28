package co.edu.cue.practicas.pattern.chain;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;

import java.util.Optional;

/**
 * GPE-145 — Contexto de validación de aptitud
 *
 * Objeto de datos que circula por la cadena de responsabilidad.
 * Agrupa todo lo que cada validador necesita para tomar su decisión,
 * evitando que los eslabones dependan entre sí.
 *
 * SOLID — SRP: solo transporta datos; no tiene lógica.
 */
public record ContextoValidacion(
        Usuario estudiante,
        CatalogoPractica catalogo,
        Optional<HojaDeVida> hvActual,
        Optional<InstanciaPractica> practicaAnterior
) {}
