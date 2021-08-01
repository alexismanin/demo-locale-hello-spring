package com.example.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

@Configuration
@EnableWebMvc
public class StaticContentConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false) // Maybe you will want to set it to true to activate cache
                .addResolver(new LocaleResolver());
    }

    /**
     * Modify static content path initially resolved by spring to include a locale dependent piece.
     */
    private class LocaleResolver implements ResourceResolver {

        @Override
        public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            final var lang = solveLang(request, "en").toLowerCase(Locale.ROOT);
            for (Resource loc: locations) {
                try {
                    final Resource langSpecializedResource = loc.createRelative(lang+"/");
                    if (langSpecializedResource.exists()) {
                        // Let spring build final path from modified prefix.
                        return chain.resolveResource(request, requestPath, List.of(langSpecializedResource));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Failure while resolving paths", e);
                }
            }

            // TODO: that does NOT propagate properly
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Language not supported");
        }

        @Override
        public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
            throw new UnsupportedOperationException("Not supported yet");
        }
    }

    /**
     * Try to extract user language from request. Search in order:
     * <ol>
     *     <li>for a <pre>lang</pre> query parameter</li>
     *     <li>for a valid <pre>Accept-Language</pre> header</li>
     * </ol>
     *
     * @param request User request to analyze.
     * @param fallback The default language value to use.
     * @return found language, or given fallback if none can be extracted from input request.
     */
    private String solveLang(final HttpServletRequest request, final String fallback) {
        if (request == null) return fallback;

        final var langs = request.getParameterValues("lang");
        if (langs != null && langs.length > 0) return langs[0];

        final var header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header != null) {
            return Locale.forLanguageTag(header).getLanguage();
        }

        return fallback;
    }
}
