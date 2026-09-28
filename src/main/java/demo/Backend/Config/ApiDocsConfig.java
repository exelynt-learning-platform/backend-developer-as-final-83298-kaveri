package demo.Backend.Config;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ApiDocsConfig implements WebMvcConfigurer {

    private final SwaggerUiConfigProperties swaggerUiConfig;
    private final SwaggerIndexTransformer swaggerIndexTransformer;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/"
                        + swaggerUiConfig.getVersion() + "/")
                .resourceChain(false)
                .addTransformer(swaggerIndexTransformer);
    }
}
