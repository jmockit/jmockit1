package mockit

import mockit.internal.UnexpectedInvocation
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Title

import java.lang.management.CompilationMXBean
import java.lang.management.ManagementFactory
import java.nio.channels.SocketChannel

@Title('Cascading Parameters Specification')
public final class CascadingParametersSpecification extends Specification {
    static class Foo {
        Bar getBar() { null }

        static Bar globalBar() { null }

        void doSomething(String s) { throw new RuntimeException(s) }

        int getIntValue() { 1 }

        Boolean getBooleanValue() { true }

        final List<Integer> getList() { null }

        HashMap<?, ?> getMap() { null }
    }

    static class Bar {
        Bar() { throw new RuntimeException() }

        int doSomething() { 1 }

        Baz getBaz() { null }

        Baz getBaz(int i) { null }

        AnEnum getEnum() { null }

        static String staticMethod() { 'notMocked' }
    }

    static final class SubBar extends Bar {}

    public interface Baz {
        void runIt()

        Date getDate()
    }

    enum AnEnum {
        First, Second, Third
    }

    @Stepwise
    @Title('Cascading Parameters Sub Specification')
    public static final class CascadingParametersSubSpecification extends Specification {
        @Shared
        Bar cascadedBar1
        @Shared
        Bar cascadedBar2

        def 'cascade one level during replay'(@Mocked Foo foo) {
            given:
            cascadedBar1 = foo.bar
            cascadedBar2 = Foo.globalBar()

            expect:
            cascadedBar1.doSomething() == 0
            cascadedBar2.doSomething() == 0

            and:
            when:
            Bar bar = foo.bar

            then:
            bar.is cascadedBar1

            and:
            when:
            Bar globalBar = Foo.globalBar()

            then:
            globalBar.is cascadedBar2
            !globalBar.is(bar)

            and:
            when:
            foo.doSomething 'test'

            then:
            noExceptionThrown()

            and:
            expect:
            foo.intValue == 0
            !foo.booleanValue
            foo.list.isEmpty()
            foo.map == null
        }

        def 'verify that previous cascaded instances have been discarded'(@Mocked Foo foo) {
            given:
            Bar bar = foo.bar
            Bar globalBar = Foo.globalBar()

            expect:
            cascadedBar1
            !bar.is(cascadedBar1)
            cascadedBar2
            !globalBar.is(cascadedBar2)
        }
    }

    def 'verify that static methods and constructors are not mocked when cascading'(@Mocked Foo foo) {
        given:
        foo.bar

        expect:
        Bar.staticMethod() == 'notMocked'

        and:
        when:
        new Bar()

        then:
        thrown RuntimeException
    }

    def 'verify that static methods and constructors are mocked when cascaded mock is mocked normally'(
            @Mocked Foo mockFoo, @Mocked Bar mockBar) {
        expect:
        mockFoo.bar.is mockBar
        mockBar.doSomething() == 0
        Bar.staticMethod() == null

        and:
        when:
        new Bar()

        then:
        noExceptionThrown()
    }

    def 'use available mocked instance of subclass as cascaded instance'(@Mocked Foo foo, @Mocked SubBar bar) {
        expect:
        foo.bar.is bar
    }

    def 'replace cascaded instance with first one of two injectable instances'(
            @Mocked final Foo foo, @Injectable final Bar bar1, @Injectable Bar bar2) {
        given:
        new GroovyExpectations() {
            {
                foo.getBar(); result = bar1
            }
        }

        expect:
        foo.bar.is bar1
        bar1.doSomething() == 0
        bar2.doSomething() == 0
    }

    def 'cascade one level during record'(@Mocked final Foo mockFoo) {
        given:
        final List<Integer> list = [1, 2, 3]

        and:
        new GroovyExpectations() {
            {
                mockFoo.doSomething anyString; minTimes = 2
                mockFoo.getBar().doSomething(); result = 2
                Foo.globalBar().doSomething(); result = 3
                mockFoo.getBooleanValue(); result = true
                mockFoo.getIntValue(); result = -1
                mockFoo.getList(); result = list
            }
        }

        and:
        Foo foo = new Foo()

        when:
        foo.doSomething '1'

        then:
        noExceptionThrown()

        and:
        expect:
        foo.bar.doSomething() == 2

        and:
        when:
        foo.doSomething '2'

        then:
        noExceptionThrown()

        and:
        expect:
        Foo.globalBar().doSomething() == 3
        foo.booleanValue
        foo.intValue == -1
        foo.list.is list
    }

    def 'cascade one level during verify'(@Mocked final Foo foo) {
        given:
        Bar bar = foo.bar
        bar.doSomething()
        bar.doSomething()
        Foo.globalBar().doSomething()

        expect:
        foo.intValue == 0
        !foo.booleanValue
        foo.list.isEmpty()

        and:
        when:
        new GroovyVerifications() {
            {
                foo.bar.doSomething(); minTimes = 2
                Foo.globalBar().doSomething(); times = 1
            }
        }

        and:
        new GroovyVerificationsInOrder() {
            {
                foo.intValue
                foo.booleanValue
            }
        }

        then:
        noExceptionThrown()
    }

    def 'cascade two levels during replay'(@Mocked Foo foo) {
        when:
        foo.bar.baz.runIt()

        then:
        noExceptionThrown()
    }

    def 'cascade two levels during record'(@Mocked final Foo mockFoo) {
        given:
        new GroovyExpectations() {
            {
                mockFoo.getBar().doSomething(); result = 1
                Foo.globalBar().doSomething(); result = 2

                mockFoo.getBar().getBaz().runIt(); times = 2
            }
        }

        and:
        Foo foo = new Foo()

        expect:
        foo.bar.doSomething() == 1
        Foo.globalBar().doSomething() == 2


        and:
        when:
        Baz baz = foo.bar.baz
        baz.runIt()
        baz.runIt()

        then:
        noExceptionThrown()
    }

    def 'cascade one level and verify invocation on last mock only'(@Mocked Foo foo, @Injectable final Bar bar) {
        given:
        Bar fooBar = foo.bar

        expect:
        fooBar.is bar

        and:
        when:
        fooBar.doSomething()

        and:
        new GroovyVerifications() {
            {
                bar.doSomething()
            }
        }

        then:
        noExceptionThrown()
    }

    def 'cascade two levels with invocation recorded on last mock only'(@Mocked Foo foo, @Mocked final Baz baz) {
        given:
        new GroovyExpectations() {
            {
                baz.runIt(); times = 1
            }
        }

        when:
        foo.bar.baz.runIt()

        then:
        noExceptionThrown()
    }

    def 'cascade two levels and verify invocation on last mock only'(@Mocked Foo foo, @Mocked final Baz baz) {
        given:
        Baz cascadedBaz = foo.bar.baz

        expect:
        cascadedBaz.is baz

        and:
        when:
        cascadedBaz.runIt()

        and:
        new GroovyVerifications() {
            {
                baz.runIt()
            }
        }

        then:
        noExceptionThrown()
    }

    // Tests using the java.lang.Process and java.lang.ProcessBuilder classes //////////////////////////////////////////

    def 'cascade on JRE classes'(@Mocked final ProcessBuilder pb) {
        given:
        new GroovyExpectations() {
            {
                ProcessBuilder sameBuilder = pb.directory(any)
                Process process = sameBuilder.start()
                process.getOutputStream().write 5
                process.exitValue(); result = 1
            }
        }

        when:
        Process process = new ProcessBuilder('test').directory(new File('myDir')).start()
        process.outputStream.write 5
        process.outputStream.flush()

        then:
        process.exitValue() == 1
    }

    def 'create os process to copy temp files'(@Mocked final ProcessBuilder pb) {
        given:
        // Code under test creates a new process to execute an OS-specific command.
        String cmdLine = 'copy /Y *.txt D:\\TEMP'
        File wrkDir = new File(/C:\TEMP/)
        Process copy = new ProcessBuilder().command(cmdLine).directory(wrkDir).start()

        expect:
        copy.waitFor() == 0

        and:
        when:
        // Verify the desired process was created with the correct command.
        new GroovyVerifications() {
            {
                pb.command(withSubstring('copy')).start()
            }
        }

        then:
        noExceptionThrown()
    }

    // Tests using java.net classes ////////////////////////////////////////////////////////////////////////////////////

    def 'record and verify expectations on cascaded mocks'(
            @Mocked Socket anySocket, @Mocked final SocketChannel cascadedChannel, @Mocked InetSocketAddress inetAddr) {
        given:
        Socket sk = new Socket()
        SocketChannel ch = sk.channel

        if (!ch.connected) {
            SocketAddress sa = new InetSocketAddress('remoteHost', 123)
            ch.connect sa
        }

        expect:
        !sk.inetAddress.is(sk.localAddress)

        and:
        when:
        new GroovyVerifications() {
            {
                cascadedChannel.connect withNotNull()
            }
        }

        then:
        noExceptionThrown()
    }

    @Stepwise
    @Title('Cascading Parameters Socket Sub Specification')
    public static final class CascadingParametersSocketSubSpecification extends Specification {
        static final class SocketFactory {
            public Socket createSocket() { return new Socket() }

            public Socket createSocket(String host, int port) throws IOException {
                return new Socket(host, port)
            }
        }

        def 'mock dynamically a class to be later mocked through cascading'() {
            given:
            new GroovyExpectations(Socket) {}
        }

        def 'cascade one level with argument matchers'(@Mocked final SocketFactory sf) {
            given:
            new GroovyExpectations() {
                {
                    sf.createSocket anyString, 80; result = null
                }
            }

            expect:
            !sf.createSocket('expected', 80)
            sf.createSocket 'unexpected', 8080
        }

        def 'record and verify one level deep'(@Mocked final SocketFactory sf) {
            given:
            final OutputStream out = new ByteArrayOutputStream()

            and:
            new GroovyExpectations() {
                {
                    sf.createSocket().getOutputStream(); result = out
                }
            }

            expect:
            sf.createSocket().outputStream.is out
        }

        def 'record and verify on two cascading mocks of the same type'(
                @Mocked final SocketFactory sf1, @Mocked final SocketFactory sf2) {
            given:
            final OutputStream out1 = new ByteArrayOutputStream()
            final OutputStream out2 = new ByteArrayOutputStream()

            and:
            new GroovyExpectations() {
                {
                    sf1.createSocket().getOutputStream(); result = out1
                    sf2.createSocket().getOutputStream(); result = out2
                }
            }

            expect:
            sf1.createSocket().outputStream.is out1
            sf2.createSocket().outputStream.is out2

            and:
            when:
            new GroovyFullVerificationsInOrder() {
                {
                    sf1.createSocket().outputStream
                    sf2.createSocket().outputStream
                }
            }

            then:
            noExceptionThrown()
        }

        def 'record and verify same invocation on mocks returned from invocations with different arguments'(
                @Mocked final SocketFactory sf) {
            given:
            new GroovyExpectations() {
                {
                    sf.createSocket().getPort(); result = 1
                    sf.createSocket('first', 80).getPort(); result = 2
                    sf.createSocket('second', 80).getPort(); result = 3
                    sf.createSocket(anyString, 81).getPort(); result = 4
                }
            }

            expect:
            sf.createSocket().port == 1
            sf.createSocket('first', 80).port == 2
            sf.createSocket('second', 80).port == 3
            sf.createSocket('third', 81).port == 4

            and:
            when:
            new GroovyVerificationsInOrder() {
                {
                    sf.createSocket().port; times = 1
                    sf.createSocket('first', 80).port
                    sf.createSocket('second', 80).port
                    sf.createSocket(anyString, 81).port; maxTimes = 1
                    sf.createSocket('fourth', -1); times = 0
                }
            }

            then:
            noExceptionThrown()
        }

        def 'cascade on inherited method'(@Mocked SocketChannel sc) {
            expect:
            sc.provider()
        }

        def 'record and verify with mixed cascade levels'(@Mocked final SocketFactory sf) {
            given:
            new GroovyExpectations() {
                {
                    sf.createSocket('first', 80).getKeepAlive(); result = true
                    sf.createSocket(withEqual('second'), anyInt).getChannel().close(); times = 1
                }
            }

            and:
            sf.createSocket('second', 80).channel.close()

            expect:
            sf.createSocket('first', 80).keepAlive

            and:
            when:
            sf.createSocket('first', 8080).channel.provider().openPipe()

            and:
            new GroovyVerifications() {
                {
                    sf.createSocket('first', 8080).channel.provider().openPipe()
                }
            }

            then:
            noExceptionThrown()
        }
    }

    def 'record strict expectation on cascaded mock'(@Mocked Foo foo, @Mocked final Bar mockBar) {
        given:
        new GroovyStrictExpectations() {
            {
                mockBar.doSomething()
            }
        }

        when:
        foo.bar.doSomething()

        then:
        noExceptionThrown()
    }

    def 'record expectation on cascaded mock'(@Mocked Foo foo, @Mocked final Bar mockBar) {
        given:
        new GroovyExpectations() {
            {
                mockBar.doSomething(); times = 1; result = 123
            }
        }

        expect:
        foo.bar.doSomething() == 123
    }

    def 'override two cascaded mocks of the same type'(
            @Mocked final Foo foo1, @Mocked final Foo foo2, @Mocked final Bar mockBar1, @Mocked final Bar mockBar2) {
        given:
        new GroovyExpectations() {
            {
                foo1.getBar(); result = mockBar1
                foo2.getBar(); result = mockBar2
                mockBar1.doSomething()
                mockBar2.doSomething()
            }
        }

        when:
        foo1.bar.doSomething()
        foo2.bar.doSomething()

        then:
        noExceptionThrown()
    }

    def 'override two cascaded mocks of the same type but replay in different order'(
            @Mocked final Foo foo1,
            @Mocked final Foo foo2, @Injectable final Bar mockBar1, @Mocked final Bar mockBar2) {
        given:
        new GroovyStrictExpectations() {
            {
                foo1.getBar(); result = mockBar1
                foo2.getBar(); result = mockBar2
                mockBar1.doSomething()
                mockBar2.doSomething()
            }
        }

        when:
        Bar bar1 = foo1.bar
        Bar bar2 = foo2.bar
        bar2.doSomething()
        bar1.doSomething()

        then:
        thrown UnexpectedInvocation
    }

    def 'cascaded enum'(@Mocked final Foo mock) {
        given:
        new GroovyExpectations() {
            {
                mock.getBar().getEnum(); result = AnEnum.Second
            }
        }

        expect:
        mock.bar.enum == AnEnum.Second
    }

    def 'cascaded enum returning consecutive values through result field'(@Mocked final Foo mock) {
        given:
        new GroovyExpectations() {
            {
                mock.getBar().getEnum()
                result = AnEnum.First
                result = AnEnum.Second
                result = AnEnum.Third
            }
        }

        expect:
        mock.bar.enum == AnEnum.First
        mock.bar.enum == AnEnum.Second
        mock.bar.enum == AnEnum.Third
    }

    def 'cascaded enum returning consecutive values through returns method'(@Mocked final Foo mock) {
        given:
        new GroovyExpectations() {
            {
                mock.getBar().getEnum()
                returns AnEnum.First, AnEnum.Second, AnEnum.Third
            }
        }

        expect:
        mock.bar.enum == AnEnum.First
        mock.bar.enum == AnEnum.Second
        mock.bar.enum == AnEnum.Third
    }

    def 'cascaded strict enum returning consecutive values through result field'(@Mocked final Foo mock) {
        given:
        new GroovyStrictExpectations() {
            {
                mock.getBar().getEnum()
                result = AnEnum.Third
                result = AnEnum.Second
                result = AnEnum.First
            }
        }

        and:
        Bar bar = mock.bar

        expect:
        bar.enum == AnEnum.Third
        bar.enum == AnEnum.Second
        bar.enum == AnEnum.First
    }

    def 'cascaded strict enum returning consecutive values through returns method'(@Mocked final Foo mock) {
        given:
        new GroovyStrictExpectations() {
            {
                mock.getBar().getEnum()
                returns AnEnum.First, AnEnum.Second, AnEnum.Third
            }
        }

        and:
        Bar bar = mock.bar

        expect:
        bar.enum == AnEnum.First
        bar.enum == AnEnum.Second
        bar.enum == AnEnum.Third
    }

    def 'override last cascaded object with non mocked instance'(@Mocked final Foo foo) {
        given:
        final Date newDate = new Date(123)

        expect:
        newDate.time == 123

        and:
        when:
        new GroovyExpectations() {
            {
                foo.getBar().getBaz().getDate()
                result = newDate
            }
        }

        then:
        new Foo().bar.baz.date.is newDate
        newDate.time == 123
    }

    def 'return Declared Mocked Instance From Multi Level Cascading'(@Mocked Date mockedDate, @Mocked Foo foo) {
        given:
        Date newDate = new Date(123)

        expect:
        newDate.time == 0

        and:
        when:
        Date cascadedDate = new Foo().bar.baz.date

        then:
        cascadedDate.is mockedDate
        newDate.time == 0
        mockedDate.time == 0
    }

    def 'return injectable mock instance from multi level cascading'(@Injectable Date mockDate, @Mocked Foo foo) {
        given:
        Date newDate = new Date(123)

        expect:
        newDate.time == 123

        and:
        when:
        Date cascadedDate = new Foo().bar.baz.date

        then:
        cascadedDate.is mockDate
        newDate.time == 123
        mockDate.time == 0
    }

    static class Factory {
        static Factory create() { null }
    }

    static class Client {
        OtherClient getOtherClient() { null }
    }

    static class OtherClient {
        static final Factory F = Factory.create()
    }

    def 'cascade during static initialization of cascading class'(@Mocked Factory mock1, @Mocked Client mock2) {
        expect:
        mock2.otherClient
        OtherClient.F
    }

    public interface LevelZero {
        Runnable getFoo()
    }

    public interface LevelOne extends LevelZero {}

    public interface LevelTwo extends LevelOne {}

    def 'create cascaded mock from method defined two levels up an interface hierarchy'(@Mocked LevelTwo mock) {
        expect:
        mock.foo
    }

    public abstract class AbstractClass implements LevelZero {}

    def 'cascade type returned from interface implemented by abstract class'(@Mocked AbstractClass mock) {
        expect:
        mock.foo
    }

    def 'produce different cascaded instances of same interface from different invocations'(@Mocked Bar bar) {
        given:
        Baz cascaded1 = bar.getBaz 1
        Baz cascaded2 = bar.getBaz 2
        Baz cascaded3 = bar.getBaz 1

        expect:
        cascaded3.is cascaded1
        !cascaded2.is(cascaded1)
    }

    def 'cascade from java management API'(@Mocked ManagementFactory mngmntFactory) {
        given:
        CompilationMXBean compilation = ManagementFactory.compilationMXBean

        expect:
        compilation
        !compilation.name
    }
}
