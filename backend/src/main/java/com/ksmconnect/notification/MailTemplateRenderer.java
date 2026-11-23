package com.ksmconnect.notification;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component
public class MailTemplateRenderer {

    private static final Locale POLISH = Locale.of("pl", "PL");

    private final TemplateEngine htmlEngine = engine(TemplateMode.HTML, ".html");
    private final TemplateEngine textEngine = engine(TemplateMode.TEXT, ".txt");

    public String renderHtml(String template, Map<String, Object> variables) {
        return htmlEngine.process(template, context(variables));
    }

    public String renderText(String template, Map<String, Object> variables) {
        return textEngine.process(template, context(variables));
    }

    private static Context context(Map<String, Object> variables) {
        Context context = new Context(POLISH);
        context.setVariables(variables);
        return context;
    }

    private static TemplateEngine engine(TemplateMode mode, String suffix) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/mail/");
        resolver.setSuffix(suffix);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
