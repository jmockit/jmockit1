package mockit

import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Title

@Title('Cascading Field Specification')
public final class CascadingFieldSpecification extends Specification {
    static class Foo {
        Bar getBar() { null }

        static Bar globalBar() { null }

        void doSomething(String s) { throw new RuntimeException(s) }

        int getIntValue() { 1 }

        Boolean getBooleanValue() { true }

        String getStringValue() { 'abc' }

        public final Date getDate() { null }

        final List<Integer> getList() { null }
    }

    static class Bar {
        Bar() { throw new RuntimeException() }

        int doSomething() { 1 }

        boolean isDone() { false }

        Short getShort() { 1 }

        List<?> getList() { null }

        Baz getBaz() { null }

        Runnable getTask() { null }
    }

    static final class Baz {
        final E e

        Baz(E e) { this.e = e }

        E getE() { e }

        void doSomething() {}
    }

    public enum E {
        A, B
    }

    public interface A {
        B getB()
    }

    public interface B {
        C getC()
    }

    public interface C {}

    @Mocked
    Foo foo
    @Mocked
    A a

    def setup() {
        new GroovyExpectations() {
            {
                Bar bar = foo.getBar(); minTimes = 0
                bar.isDone(); result = true; minTimes = 0
            }
        }
    }

    def 'obtain cascaded instances at all levels'() {
        expect:
        foo.bar
        foo.bar.list != null
        foo.bar.baz
        foo.bar.task

        and:
        when:
        B b = a.b

        then:
        b
        b.c
    }

    def 'obtain cascaded instances at all levels again'() {
        when:
        Bar bar = foo.bar

        then:
        bar
        bar.list != null
        bar.baz
        bar.task

        and:
        expect:
        a.b
        a.b.c
    }

    def 'cascade one level'() {
        expect:
        foo.bar.done
        foo.bar.doSomething() == 0
        Foo.globalBar().doSomething() == 0
        !Foo.globalBar().is(foo.bar)
        foo.bar.short.intValue() == 0

        and:
        when:
        foo.doSomething 'test'

        then:
        noExceptionThrown()

        and:
        expect:
        foo.intValue == 0
        !foo.booleanValue
        foo.stringValue == null
        foo.date
        foo.list.isEmpty()

        and:
        when:
        new GroovyVerifications() {
            {
                foo.doSomething anyString
            }
        }

        then:
        noExceptionThrown()
    }

    def 'exercise cascading mock again'() {
        expect:
        foo.bar.done
    }

    def 'record unambiguous strict expectations producing different cascaded instances'() {
        given:
        new GroovyStrictExpectations() {
            {
                Bar c1 = Foo.globalBar()
                c1.isDone(); result = true
                Bar c2 = Foo.globalBar()
                c2.doSomething(); result = 5
                if (c1.is(c2)) {
                    throw new AssertionError('c1 and c2 should be different')
                }
            }
        }

        when:
        Bar b1 = Foo.globalBar()

        then:
        b1.done

        and:
        when:
        Bar b2 = Foo.globalBar()

        then:
        b2.doSomething() == 5

        and:
        !b1.is(b2)
    }

    def 'record unambiguous non strict expectations producing different cascaded instances'(
            @Mocked final Foo foo1, @Mocked final Foo foo2) {
        given:
        new GroovyExpectations() {
            {
                Date c1 = foo1.getDate()
                Date c2 = foo2.getDate()
                if (c1.is(c2)) {
                    throw new AssertionError('c1 and c2 should be different')
                }
            }
        }

        when:
        Date d1 = foo1.date
        Date d2 = foo2.date

        then:
        !d1.is(d2)
    }

    def 'record ambiguous expectations on instance method producing the same cascaded instance'() {
        given:
        new GroovyExpectations() {
            {
                Bar c1 = foo.getBar()
                Bar c2 = foo.getBar()
                if (!c1.is(c2)) {
                    throw new AssertionError('c1 and c2 should be identical')
                }
            }
        }

        when:
        Bar b1 = foo.bar
        Bar b2 = foo.bar

        then:
        b1.is b2
    }

    def 'record ambiguous expectations on static method producing the same cascaded instance'() {
        given:
        new GroovyExpectations() {
            {
                Bar c1 = Foo.globalBar()
                Bar c2 = Foo.globalBar()
                if (!c1.is(c2)) {
                    throw new AssertionError('c1 and c2 should be identical')
                }
            }
        }

        when:
        Bar b1 = Foo.globalBar()
        Bar b2 = Foo.globalBar()

        then:
        b1.is b2
    }

    static final class AnotherFoo {
        Bar getBar() { null }
    }
    @Mocked
    AnotherFoo anotherFoo

    def 'cascading mock field'() {
        given:
        new GroovyExpectations() {
            {
                anotherFoo.getBar().doSomething(); result = 123
            }
        }

        expect:
        new AnotherFoo().bar.doSomething() == 123
    }

    def 'cascading instance accessed from delegate method'() {
        given:
        new GroovyExpectations() {
            {
                foo.getIntValue()
                result = new Delegate() {
                    @Mock
                    int delegate() { foo.bar.doSomething() }
                }
            }
        }

        expect:
        foo.intValue == 0
    }

    @Mocked
    BazCreatorAndConsumer bazCreatorAndConsumer

    static class BazCreatorAndConsumer {
        Baz create() { null }

        void consume(Baz arg) { arg.toString() }
    }

    def 'call method on non cascaded instance from custom argument matcher with cascaded instance also created'() {
        when:
        Baz nonCascadedInstance = new Baz(E.A)
        Baz cascadedInstance = bazCreatorAndConsumer.create()

        then:
        !nonCascadedInstance.is(cascadedInstance)

        when:
        bazCreatorAndConsumer.consume(nonCascadedInstance);

        and:
        new GroovyVerifications() {
            {
                bazCreatorAndConsumer.consume(with(new Delegate<Baz>() {
                    boolean matches(Baz actual) { actual.e.is E.A }
                }))
            }
        }

        then:
        noExceptionThrown()
    }

    // Tests for cascaded instances obtained from generic methods //////////////////////////////////////////////////////

    static class GenericBaseClass1<T> {
        T getValue() { null }
    }

    def 'cascade generic method from specialized generic class'(@Mocked GenericBaseClass1<C> mock) {
        expect:
        mock.value
    }

    static class ConcreteSubclass1 extends GenericBaseClass1<A> {}

    def 'cascade generic method of concrete subclass which extends generic class'(
            @Mocked final ConcreteSubclass1 mock) {
        given:
        new GroovyExpectations() {
            {
                mock.getValue().getB().getC()
                result = new C() {}
            }
        }

        expect:
        A value = mock.value
        value
        B b = value.b
        b
        b.c

        and:
        when:
        new GroovyFullVerificationsInOrder() {
            {
                mock.value.b.c
            }
        }

        then:
        noExceptionThrown()
    }

    interface Ab extends A {}

    static class GenericBaseClass2<T extends A> {
        T getValue() { null }
    }

    static class ConcreteSubclass2 extends GenericBaseClass2<Ab> {}

    def 'cascade generic method of subclass which extends generic class with upper bound using interface'(
            @Mocked final ConcreteSubclass2 mock) {
        given:
        Ab value = mock.value

        expect:
        value
        value.b.c

        and:
        when:
        new GroovyVerifications() {
            {
                mock.value.b.c; times = 1
            }
        }

        then:
        noExceptionThrown()
    }

    def 'cascade generic method of subclass which extends generic class with upper bound only in verification block'(
            @Mocked final ConcreteSubclass2 mock) {
        when:
        new GroovyFullVerifications() {
            {
                Ab value = mock.value; times = 0
                B b = value.b; times = 0
                b.c; times = 0
            }
        }

        then:
        noExceptionThrown()
    }

    static final class Action implements A {
        @Override
        public B getB() { null }
    }

    static final class ActionHolder extends GenericBaseClass2<Action> {}

    def 'cascade generic method of subclass which extends generic class with upper bound using class'(
            @Mocked final ActionHolder mock) {
        given:
        new GroovyStrictExpectations() {
            {
                mock.value.b.c
            }
        }

        when:
        mock.value.b.c

        then:
        noExceptionThrown()
    }

    @Stepwise
    @Title('Cascading Field Sub Specification')
    public static final class CascadingFieldSubSpecification extends Specification {
        class Base<T extends Serializable> {
            T value() { 123 as T }
        }

        class Derived1 extends Base<Long> {}

        class Derived2 extends Base<Long> {}

        interface Factory1 {
            Derived1 get1()
        }

        interface Factory2 {
            Derived2 get2()
        }

        @Mocked
        Factory1 factory1
        @Mocked
        Factory2 factory2

        def 'use subclass mocked through cascading'() {
            expect:
            factory1.get1().value() == 0L // cascade-mocks Derived1 (per-instance)
            new Derived1().value() == 123L // new instance, not mocked
        }

        def 'use subclass previously mocked through cascading while mocking sibling subclass'(@Injectable Derived2 d2) {
            expect:
            new Derived1().value() == 123L
            d2.value() == 0L
            new Derived2().value() == 123L
        }
    }
}
