package com.poli.restaurante.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra el interceptor de autenticación para toda la API (menos el inicio de sesión) y enseña a
 * Spring a llenar los parámetros {@code @UsuarioActual Long} y {@code Sesion} de los controladores.
 */
@Configuration
@RequiredArgsConstructor
public class SeguridadConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacion;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacion)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SesionArgumentResolver());
    }

    static class SesionArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType() == Sesion.class
                    || (parameter.hasParameterAnnotation(UsuarioActual.class)
                        && parameter.getParameterType() == Long.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            Sesion sesion = (Sesion) webRequest.getAttribute(Sesion.ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
            if (sesion == null) {
                throw new IllegalStateException("La ruta " + parameter.getExecutable().getName()
                        + " no pasa por el interceptor de autenticación");
            }
            return parameter.getParameterType() == Sesion.class ? sesion : sesion.usuarioId();
        }
    }
}
