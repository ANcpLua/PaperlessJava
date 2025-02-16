package at.fhtw.rest.unit;

import at.fhtw.rest.infrastructure.CorsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class CorsConfigTest {

    private WebMvcConfigurer webMvcConfigurer;

    @BeforeEach
    void setUp() {
        CorsConfig corsConfig = new CorsConfig();
        webMvcConfigurer = corsConfig.corsConfigurer();
    }

    @Test
    @DisplayName("corsConfigurer() should configure CORS mappings correctly")
    void testCorsMappingsAreConfigured() {
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOrigins("*")).thenReturn(registration);
        when(registration.allowedMethods("*")).thenReturn(registration);
        when(registration.allowedHeaders("*")).thenReturn(registration);

        webMvcConfigurer.addCorsMappings(registry);

        verify(registry, times(1)).addMapping("/**");
        verify(registration, times(1)).allowedOrigins("*");
        verify(registration, times(1)).allowedMethods("*");
        verify(registration, times(1)).allowedHeaders("*");
    }
}