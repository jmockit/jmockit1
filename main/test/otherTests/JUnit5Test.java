package otherTests;

import mockit.ClassWithObjectOverrides;
import mockit.Mocked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class JUnit5Test {
    @Mocked
    ClassWithObjectOverrides mock;

    @Test
    void shouldInitializeArg(@Mocked final ClassWithObjectOverrides mock) {
        Assertions.assertNotNull(mock);
    }

    @Test
    void shouldInitializeField() {
        Assertions.assertNotNull(mock);
    }

    abstract static class TestTemplate {
        @Mocked
        ClassWithObjectOverrides mock;
    }

    @Nested
    class NestedWithArg {
        @Test
        void shouldInitializeArg(@Mocked final ClassWithObjectOverrides mock) {
            Assertions.assertNotNull(mock);
        }
    }

    @Nested
    class NestedWithField {
        @Mocked
        ClassWithObjectOverrides mock;

        @Test
        void shouldInitializeField() {
            Assertions.assertNotNull(mock);
        }
    }

    @Nested
    class NestedWithInheritedField extends TestTemplate {
        @Test
        void shouldInitializeInheritedField() {
            Assertions.assertNotNull(mock);
        }
    }

    @Nested
    class NestedWithFieldAccessingTopLevelField {
        @Mocked
        ClassWithObjectOverrides second;

        @Test
        void shouldInitializeField() {
            Assertions.assertNotNull(mock);
            Assertions.assertNotSame(mock, second);
        }
    }
}
