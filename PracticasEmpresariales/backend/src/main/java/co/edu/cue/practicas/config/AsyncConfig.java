package co.edu.cue.practicas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async para los listeners del patrón Observer */
@Configuration
@EnableAsync
public class AsyncConfig {
}
