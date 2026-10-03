package com.ensar.clmp.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

/**
 * Constitution IV / research R12: REST controllers never expose JPA entities. Fails if any public
 * controller method mentions an {@code @Entity} type in its return type, generic arguments, or
 * parameters.
 */
class NoEntityInControllerSignatureTest {

    @Test
    void controllersNeverExposeEntities() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinition> controllers = scanner.findCandidateComponents("com.ensar.clmp");
        assertThat(controllers).as("at least one controller is scanned").isNotEmpty();

        List<String> violations = new ArrayList<>();
        for (BeanDefinition definition : controllers) {
            Class<?> controller = ClassUtils.forName(definition.getBeanClassName(), getClass().getClassLoader());
            for (Method method : controller.getDeclaredMethods()) {
                if (Modifier.isPrivate(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }
                checkType(ResolvableType.forMethodReturnType(method), controller, method, violations);
                for (int i = 0; i < method.getParameterCount(); i++) {
                    checkType(ResolvableType.forMethodParameter(new MethodParameter(method, i)), controller, method,
                            violations);
                }
            }
        }
        assertThat(violations).as("controller signatures exposing @Entity types").isEmpty();
    }

    private static void checkType(ResolvableType type, Class<?> controller, Method method, List<String> violations) {
        collect(type, new HashSet<>()).stream()
                .filter(c -> c.isAnnotationPresent(Entity.class))
                .forEach(c -> violations.add(controller.getSimpleName() + "#" + method.getName() + " -> "
                        + c.getSimpleName()));
    }

    private static Set<Class<?>> collect(ResolvableType type, Set<Class<?>> seen) {
        Class<?> raw = type.resolve();
        if (raw != null && seen.add(raw)) {
            if (raw.isArray()) {
                collect(type.getComponentType(), seen);
            }
        }
        for (ResolvableType generic : type.getGenerics()) {
            collect(generic, seen);
        }
        return seen;
    }
}
