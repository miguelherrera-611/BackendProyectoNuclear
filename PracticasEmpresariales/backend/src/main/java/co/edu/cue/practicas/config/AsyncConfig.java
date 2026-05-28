package co.edu.cue.practicas.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async para los listeners del patrón Observer.
 * Habilita @Cacheable/@CacheEvict para el patrón Proxy Caché del expediente.
 */
@Configuration
@EnableAsync
@EnableCaching
public class AsyncConfig {
}
