package com.sequenceiq.cloudbreak;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.util.ReflectionUtils;

class DomainToStringTest {

    private static final Set<Class<?>> ENTITY_CLASSES = new Reflections("com.sequenceiq", new TypeAnnotationsScanner(), new SubTypesScanner())
            .getTypesAnnotatedWith(Entity.class);

    @Test
    void testMissingToString() {
        assertSoftly(softly ->
                ENTITY_CLASSES.forEach(entityClass -> {
                    Object entityInstance = createEntityInstance(entityClass);
                    String result = entityInstance.toString();
                    softly.assertThat(result).doesNotStartWith(entityClass.getName() + "@");
                }));
    }

    @Test
    void testLazyLoadSafeToString() {
        assertSoftly(softly ->
                ENTITY_CLASSES.forEach(entityClass -> {
                    Object entityInstance = createEntityInstance(entityClass);
                    setLazyLoadedFields(entityClass, entityInstance);
                    try {
                        entityInstance.toString();
                    } catch (UninitializedToString e) {
                        softly.fail("Lazy loaded field %s may throw UninitializedException, please wrap in DatabaseUtil.lazyLoadSafeToString()", e.getField());
                    }
                }));
    }

    /**
     * Set circular entity references, so if they are referenced in each other's toString() methods, the test will fail with a {@link StackOverflowError}
     */
    @Test
    void testCircularEntityReferencesInToString() {
        Map<? extends Class<?>, Object> entityInstancesByClass = ENTITY_CLASSES.stream()
                .map(this::createEntityInstance)
                .collect(Collectors.toMap(Object::getClass, Function.identity()));

        entityInstancesByClass.values().forEach(entity -> Stream.of(entity.getClass().getDeclaredFields())
                .filter(field -> entityInstancesByClass.containsKey(field.getType()))
                .forEach(field -> {
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, entity, entityInstancesByClass.get(field.getType()));
                }));

        assertSoftly(softly -> entityInstancesByClass.values()
                .forEach(entityInstance -> softly.assertThatCode(entityInstance::toString).doesNotThrowAnyException()));
    }

    private Object createEntityInstance(Class<?> entityClass) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify toString() implementation of entity " + entityClass.getName(), e);
        }
    }

    private void setLazyLoadedFields(Class<?> entityClass, Object newInstance) {
        Set<Field> lazyLoadedFields = Stream.of(entityClass.getDeclaredFields())
                .filter(this::isLazyLoaded)
                .collect(Collectors.toSet());
        lazyLoadedFields.forEach(field -> {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, newInstance, createProxyObject(field.getType(), field));
        });
    }

    private boolean isLazyLoaded(Field field) {
        return (field.isAnnotationPresent(OneToMany.class) && FetchType.LAZY.equals(field.getAnnotation(OneToMany.class).fetch()))
                || (field.isAnnotationPresent(OneToOne.class) && FetchType.LAZY.equals(field.getAnnotation(OneToOne.class).fetch()))
                || (field.isAnnotationPresent(ManyToOne.class) && FetchType.LAZY.equals(field.getAnnotation(ManyToOne.class).fetch()));
    }

    private <T> T createProxyObject(Class<T> type, Field field) {
        LazyInitializer lazyInitializer = mock(LazyInitializer.class);
        when(lazyInitializer.isUninitialized()).thenReturn(true);
        T hibernateProxy = mock(type, withSettings().extraInterfaces(HibernateProxy.class));
        when(((HibernateProxy) hibernateProxy).getHibernateLazyInitializer()).thenReturn(lazyInitializer);
        when(hibernateProxy.toString()).thenThrow(new UninitializedToString(field));
        return hibernateProxy;
    }

    static class UninitializedToString extends RuntimeException {
        private final Field field;

        UninitializedToString(Field field) {
            this.field = field;
        }

        public Field getField() {
            return field;
        }
    }
}
