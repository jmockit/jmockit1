package mockit

import mockit.internal.ClassFile
import spock.lang.Specification
import spock.lang.Title

import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean

@Title('Capturing Implementations Specification')
public final class CapturingImplementationsSpecification extends Specification {
    interface ServiceToBeStubbedOut {
        int doSomething()
    }

    // Just to cause any implementing classes to be stubbed out.
    @Capturing
    ServiceToBeStubbedOut unused

    static final class ServiceLocator {
        @SuppressWarnings("GroovyUnusedDeclaration")
        static <S> S getInstance(Class<S> serviceInterface) {
            new ServiceToBeStubbedOut() {
                @Override
                public int doSomething() { 10 }
            } as S
        }
    }

    def 'capture implementation loaded by service locator'() {
        expect:
        ServiceLocator.getInstance(ServiceToBeStubbedOut).doSomething() == 0
    }

    public interface Service1 {
        int doSomething()
    }

    static final class Service1Impl implements Service1 {
        @Override
        public int doSomething() { 1 }
    }

    @Capturing
    Service1 mockService1

    def 'capture implementation using mock field'() {
        given:
        Service1 service = new Service1Impl()

        and:
        new GroovyExpectations() {
            {
                mockService1.doSomething()
                returns 2, 3
            }
        }

        expect:
        service.doSomething() == 2
        new Service1Impl().doSomething() == 3
    }

    public interface Service2 {
        int doSomething()
    }

    static final class Service2Impl implements Service2 {
        @Override
        public int doSomething() { 1 }
    }

    def 'capture implementation using mock parameter'(@Capturing final Service2 mock) {
        given:
        Service2Impl service = new Service2Impl()

        and:
        new GroovyExpectations() {
            {
                mock.doSomething()
                returns 3, 2
            }
        }

        expect:
        service.doSomething() == 3
        new Service2Impl().doSomething() == 2
    }

    public abstract static class AbstractService {
        protected abstract boolean doSomething()
    }

    static final class DefaultServiceImpl extends AbstractService {
        @Override
        protected boolean doSomething() { true }
    }

    def 'capture implementation of abstract class'(@Capturing AbstractService mock) {
        expect:
        !new DefaultServiceImpl().doSomething()
        !new AbstractService() {
            @Override
            protected boolean doSomething() { throw new RuntimeException() }
        }.doSomething()
    }

    def 'capture generated mock subclass'(@Capturing final AbstractService mock1, @Mocked final AbstractService mock2) {
        given:
        new GroovyExpectations() {
            {
                mock1.doSomething(); result = true
                mock2.doSomething(); result = false
            }
        }

        expect:
        !mock2.doSomething()
        mock1.doSomething()
        new DefaultServiceImpl().doSomething()
    }

    static final Class<? extends Service2> customLoadedClass = new ClassLoader() {
        @Override
        protected Class<? extends Service2> findClass(String name) {
            byte[] bytecode = ClassFile.readFromFile(name.replace('.', '/')).b
            defineClass name, bytecode, 0, bytecode.length
        }
    }.findClass Service2Impl.name

    final Service2 service2 = Deencapsulation.newInstance customLoadedClass

    def 'capture class previously loaded by class loader other than context'(@Capturing final Service2 mock) {
        given:
        new GroovyExpectations() {
            {
                mock.doSomething(); result = 15
            }
        }

        expect:
        service2.doSomething() == 15
    }

    @Capturing
    ServiceDoSomething mockService2

    def setupSpec() {
        ServiceDoSomething.ServiceDoSomethingProvider.proxyInstance =
                ServiceDoSomething.ServiceDoSomethingProvider.newProxyClassAndInstance ServiceDoSomething, Serializable
    }

    def 'capture dynamically generated proxy class'() {
        expect:
        ServiceDoSomething.ServiceDoSomethingProvider.proxyInstance.doSomething() == 0

        when:
        new GroovyExpectations() {
            {
                mockService2.doSomething(); result = 123
            }
        }

        then:
        ServiceDoSomething.ServiceDoSomethingProvider.proxyInstance.doSomething() == 123
        ServiceDoSomething.ServiceDoSomethingProvider.newProxyClassAndInstance(ServiceDoSomething).doSomething() == 123
    }

    interface Interface {
        void op()
    }

    interface SubInterface extends Interface {}

    static class Implementation implements SubInterface {
        @Override
        public void op() { throw new RuntimeException() }
    }

    def 'capture class implementing sub interface of captured interface'(@Capturing Interface base) {
        when:
        new Implementation().op()

        then:
        noExceptionThrown()
    }

    def 'capture classes from the java management api'(@Capturing ThreadMXBean anyThreadMXBean) {
        given:
        ThreadMXBean threadingBean = ManagementFactory.threadMXBean

        expect:
        threadingBean.threadCount == 0
    }

    interface Interface2 {
        int doSomething()
    }

    interface SubInterface2 extends Interface2 {}

    static class ClassImplementingSubInterfaceAndExtendingUnrelatedBase extends Implementation implements SubInterface2 {
        @Override
        public int doSomething() { 123 }
    }

    def 'capture class which implements captured base interface and extends unrelated base'(
            @Capturing Interface2 captured) {
        expect:
        new ClassImplementingSubInterfaceAndExtendingUnrelatedBase().doSomething() == 0
    }

    static class Base<T> {
        T doSomething() { null }

        void doSomething(T t) { println 'test' }
    }

    static final class Impl extends Base<Integer> {
        @Override
        Integer doSomething() { 1 }

        @Override
        void doSomething(Integer i) {}
    }

    def 'capture implementations of generic type'(@Capturing final Base<Integer> anyInstance) {
        given:
        new GroovyExpectations() {
            {
                anyInstance.doSomething(); result = 2
                anyInstance.doSomething 0
            }
        }

        when:
        Base<Integer> impl = new Impl()
        int i = impl.doSomething()
        impl.doSomething 0

        then:
        i == 2
    }
}
