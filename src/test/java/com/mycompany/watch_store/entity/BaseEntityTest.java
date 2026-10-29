package com.mycompany.watch_store.entity;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 30, 9, 0);

    @Test
    void defaultConstructorInitializesCreationTime() {
        LocalDateTime before = LocalDateTime.now();
        TestEntity entity = new TestEntity();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(entity.getCreatedAt());
        assertTrue(!entity.getCreatedAt().isBefore(before));
        assertTrue(!entity.getCreatedAt().isAfter(after));
    }

    @Test
    void newEntityHasZeroId() {
        assertEquals(0, new TestEntity().getId());
    }

    @Test
    void parameterizedConstructorInitializesBothAttributes() {
        TestEntity entity = new TestEntity(7, CREATED_AT);

        assertEquals(7, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void parameterizedConstructorRejectsNonpositiveId(int id) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TestEntity(id, CREATED_AT));

        assertEquals("Existing entity ID must be greater than 0.", exception.getMessage());
    }

    @Test
    void parameterizedConstructorRejectsNullCreationTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TestEntity(7, null));

        assertEquals("Creation time must not be null.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void setIdRejectsNonpositiveIdWithoutChangingState(int id) {
        TestEntity entity = new TestEntity(7, CREATED_AT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> entity.setId(id));

        assertEquals("Entity ID must be greater than 0.", exception.getMessage());
        assertEquals(7, entity.getId());
    }

    @Test
    void setCreatedAtRejectsNullWithoutChangingState() {
        TestEntity entity = new TestEntity(7, CREATED_AT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> entity.setCreatedAt(null));

        assertEquals("Creation time must not be null.", exception.getMessage());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    void validSettersUpdateBothAttributes() {
        TestEntity entity = new TestEntity();

        entity.setId(12);
        entity.setCreatedAt(CREATED_AT);

        assertEquals(12, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    void subclassInheritsPublicMethods() throws NoSuchMethodException {
        BaseEntity entity = new TestEntity();
        entity.setId(3);
        entity.setCreatedAt(CREATED_AT);

        assertEquals(3, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(BaseEntity.class, TestEntity.class.getMethod("getId").getDeclaringClass());
    }

    @Test
    void baseEntityIsAbstract() {
        assertTrue(Modifier.isAbstract(BaseEntity.class.getModifiers()));
        // new BaseEntity() is rejected by the Java compiler because BaseEntity is abstract.
    }

    private static final class TestEntity extends BaseEntity {
        private TestEntity() {
            super();
        }

        private TestEntity(int id, LocalDateTime createdAt) {
            super(id, createdAt);
        }
    }
}
