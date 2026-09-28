package demo.Backend.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SwaggerThemeIndexTransformer extends SwaggerIndexPageTransformer {

    public SwaggerThemeIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
            throws IOException {
        if (!"index.html".equals(resource.getFilename())) {
            return super.transform(request, resource, transformerChain);
        }

        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String html = read(resource);
        String injection = """
                <script>
                (function () {
                  try {
                    var saved = localStorage.getItem("swagger-theme");
                    var theme = saved === "light" || saved === "dark"
                      ? saved
                      : (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
                    document.documentElement.setAttribute("data-theme", theme);
                  } catch (e) {}
                })();
                </script>
                <link rel="stylesheet" href="%s/swagger-theme/theme.css?v=4" />
                """.formatted(contextPath);
        html = html.replace("<head>", "<head>" + injection);
        html = html.replace(
                "</body>",
                "<script src=\"" + contextPath + "/swagger-theme/theme.js?v=4\"></script></body>");
        return new TransformedResource(resource, html.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
