package cl.camodev.wosbot.web.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import cl.camodev.wosbot.web.logging.WebRequestLoggingInterceptor;

/**
 * Spring MVC configuration for the web dashboard.
 * Configures Gson for JSON serialization and static resource serving.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    private final WebRequestLoggingInterceptor webRequestLoggingInterceptor;

    public WebConfig(WebRequestLoggingInterceptor webRequestLoggingInterceptor) {
        this.webRequestLoggingInterceptor = webRequestLoggingInterceptor;
    }

    
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        GsonHttpMessageConverter gsonConverter = new GsonHttpMessageConverter();
        gsonConverter.setGson(JsonSerializerConfig.getGson());
        converters.add(gsonConverter);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources from classpath:/static/
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Keep SSE connections alive without server-side timeouts
        configurer.setDefaultTimeout(-1L);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String corsOrigins = corsAllowedOrigins;
        if (StringUtils.hasText(corsOrigins)) {
            corsOrigins = corsOrigins + ",http://localhost:8000,http://127.0.0.1:8000";
        } else {
            corsOrigins = "http://localhost:8000,http://127.0.0.1:8000";
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(Arrays.stream(corsOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        // Enable redirect: and forward: prefix handling
        resolver.setRedirectHttp10Compatible(false);
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webRequestLoggingInterceptor);
    }

    private static class SpaFallbackResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requestedResource = location.createRelative(resourcePath);
            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }

            if (resourcePath.startsWith("api")) {
                return null;
            }

            return location.createRelative("index.html");
        }
    }


}
