package com.ecole.gestion_scolaire.audit.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class AuditWebConfig implements WebMvcConfigurer {
 private final AuditInterceptor interceptor; public AuditWebConfig(AuditInterceptor interceptor){this.interceptor=interceptor;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor).addPathPatterns("/api/**");}
}
