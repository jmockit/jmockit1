/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit

import spock.lang.Specification
import spock.lang.Title

import java.util.concurrent.Callable

@Title('Capturing Instances Specification')
public final class CapturingInstancesSpecification extends Specification {
    public interface Service1 {
        int doSomething()
    }

    static final class Service1Impl implements Service1 {
        @Override
        public int doSomething() { 1 }
    }

    public static final class TestedUnit {
        private final Service1 service1 = new Service1Impl()
        private final Service1 service2 = new Service1() {
            @Override
            public int doSomething() { 2 }
        }
        Observable observable

        public int businessOperation(final boolean b) {
            new Callable() {
                @Override
                public Object call() { throw new IllegalStateException() }
            }.call()

            observable = new Observable() {
                {
                    if (b) {
                        throw new IllegalArgumentException()
                    }
                }
            }

            service1.doSomething() + service2.doSomething()
        }
    }

    @Capturing(maxInstances = 2)
    Service1 service

    def 'capture service instances created by tested constructor'() {
        given:
        TestedUnit unit = new TestedUnit()

        expect:
        unit.service1.doSomething() == 0
        unit.service2.doSomething() == 0
    }

    def 'capture all internally created instances'(@Capturing Observable observable, @Capturing Callable callable) {
        given:
        new GroovyExpectations() {
            {
                service.doSomething(); returns 3, 4
            }
        }

        and:
        TestedUnit unit = new TestedUnit();

        expect:
        unit.businessOperation(true) == 7
        unit.service1.doSomething() == 4
        unit.service2.doSomething() == 4
        unit.observable

        and:
        when:
        new GroovyVerifications() {
            {
                callable.call()
            }
        }

        then:
        noExceptionThrown()
    }

    public interface Service2 {
        int doSomething()
    }

    static final class Service2Impl implements Service2 {
        @Override
        public int doSomething() { 2 }
    }

    def 'record strict expectations for next two instances to be created'(
            @Capturing(maxInstances = 1) final Service2 s1, @Capturing(maxInstances = 1) final Service2 s2) {
        given:
        new GroovyStrictExpectations() {
            {
                s1.doSomething(); result = 11
                s2.doSomething(); returns 22, 33
            }
        }

        and:
        Service2Impl service1 = new Service2Impl()
        Service2Impl service2 = new Service2Impl()

        expect:
        service1.doSomething() == 11
        service2.doSomething() == 22
        service2.doSomething() == 33
    }

    def 'record expectations for next two instances to be created'(
            @Capturing(maxInstances = 1) final Service2 mock1, @Capturing(maxInstances = 1) final Service2 mock2) {
        given:
        new GroovyExpectations() {
            {
                mock1.doSomething(); result = 11
                mock2.doSomething(); result = 22
            }
        }

        and:
        Service2Impl s1 = new Service2Impl()
        Service2Impl s2 = new Service2Impl()

        expect:
        s2.doSomething() == 22
        s1.doSomething() == 11
        s1.doSomething() == 11
        s2.doSomething() == 22
        s1.doSomething() == 11
    }

    def 'record expectations for next two instances of two different implementing classes'(
            @Capturing(maxInstances = 1) final Service2 mock1, @Capturing(maxInstances = 1) final Service2 mock2) {
        given:
        new GroovyExpectations() {
            {
                mock1.doSomething(); result = 1
                mock2.doSomething(); result = 2
            }
        }

        and:
        Service2 s1 = new Service2() {
            @Override
            public int doSomething() { -1 }
        }
        Service2 s2 = new Service2() {
            @Override
            public int doSomething() { -2 }
        }

        expect:
        s1.doSomething() == 1
        s2.doSomething() == 2
    }

    def 'record expectations for two consecutive sets of future instances'(
            @Capturing(maxInstances = 2) final Service2 set1, @Capturing(maxInstances = 3) final Service2 set2) {
        given:
        new GroovyExpectations() {
            {
                set1.doSomething(); result = 1
                set2.doSomething(); result = 2
            }
        }

        and: 'First set of instances, matching the expectation on "set1"'
        Service2 s1 = new Service2Impl();
        Service2 s2 = new Service2Impl();

        and: 'Second set of instances, matching the expectation on "set2"'
        Service2 s3 = new Service2Impl();
        Service2 s4 = new Service2Impl();
        Service2 s5 = new Service2Impl();

        and: 'Third set of instances, not matching any expectation'
        Service2 s6 = new Service2Impl();

        expect:
        s1.doSomething() == 1
        s2.doSomething() == 1

        and:
        s3.doSomething() == 2
        s4.doSomething() == 2
        s5.doSomething() == 2

        and:
        s6.doSomething() == 0
    }

    def 'record expectations for next two instances to be created using mock parameters'(
            @Capturing(maxInstances = 1) final Service2 s1, @Capturing(maxInstances = 1) final Service2 s2) {
        given:
        new GroovyExpectations() {
            {
                s2.doSomething(); result = 22
                s1.doSomething(); result = 11
            }
        }

        and:
        Service2Impl cs1 = new Service2Impl();
        Service2Impl cs2 = new Service2Impl();

        expect:
        cs1.doSomething() == 11
        cs2.doSomething() == 22
        cs1.doSomething() == 11
        cs2.doSomething() == 22
    }

    static class Base {
        boolean doSomething() { false }
    }

    static final class Derived1 extends Base {}

    static final class Derived2 extends Base {
        Service2 doSomethingElse() { null }
    }

    def 'verify expectations only on first of two captured instances'(@Capturing(maxInstances = 1) final Base b) {
        given:
        new GroovyExpectations() {
            {
                b.doSomething(); result = true; times = 1
            }
        }

        expect:
        new Derived1().doSomething()
        !new Derived2().doSomething()
    }

    def 'verify expectations only on one of two subclasses for two captured instances'(
            @Capturing(maxInstances = 1) final Derived1 firstCapture,
            @Capturing(maxInstances = 1) final Derived1 secondCapture) {
        given:
        new GroovyExpectations() {
            {
                new Derived1(); times = 2
                firstCapture.doSomething(); result = true; times = 1
                secondCapture.doSomething(); result = true; times = 1
            }
        }

        expect:
        new Derived1().doSomething()
        !new Derived2().doSomething()
        new Derived1().doSomething()
    }

    def 'capture subclass and cascade from method exclusive to subclass'(@Capturing Base capturingMock) {
        given:
        Derived2 d = new Derived2()

        expect:
        // Classes mocked only because they implement / extend a capturing base type do not cascade from methods
        // that exist only in them.
        d.doSomethingElse() == null
    }

    abstract class Buffer {
        abstract int position()
    }

    class ByteBuffer extends Buffer {
        int position() { 1 }
    }

    class IntBuffer extends Buffer {
        int position() { 2 }
    }

    class CharBuffer extends Buffer {
        int position() { 3 }
    }

    // cannot use java.nio.Buffer as original Java test, because added Groovy magic
    // does load some classes which creates various Buffer-subclass instances
    // this works against the maxInstances count and thus custom objects are used
    def 'specify different behavior for first new instance and for remaining new instances'(
            @Capturing(maxInstances = 1) final Buffer firstNewBuffer, @Capturing final Buffer remainingNewBuffers) {
        given:
        new GroovyExpectations() {
            {
                firstNewBuffer.position(); result = 10
                remainingNewBuffers.position(); result = 20
            }
        }

        when:
        ByteBuffer buffer1 = new ByteBuffer()
        IntBuffer buffer2 = new IntBuffer()
        CharBuffer buffer3 = new CharBuffer()

        then:
        buffer1.position() == 10
        buffer2.position() == 20
        buffer3.position() == 20
    }
}