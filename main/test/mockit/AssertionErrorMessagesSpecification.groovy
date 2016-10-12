package mockit

import mockit.internal.MissingInvocation
import mockit.internal.UnexpectedInvocation
import mockit.internal.expectations.RecordAndReplayExecution
import mockit.internal.state.TestRun
import spock.lang.Specification
import spock.lang.Title

@Title('Assertion Error Messages Specification')
public final class AssertionErrorMessagesSpecification extends Specification {
    @SuppressWarnings("GroovyUnusedDeclaration")
    static class Collaborator {
        void doSomething() {}

        void doSomething(int i, String s) {}

        void doSomethingElse(String s) {}
    }

    @Mocked
    Collaborator mock

    def 'unexpected invocation for recorded strict expectation'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomething anyInt, anyString
                returns 5
            }
        }

        when:
        mock.doSomething 1, 'Abc'
        mock.doSomething 2, 'xyz'

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 2, "xyz"'
    }

    def 'unexpected invocation where expecting another for recorded strict expectations'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomething anyInt, anyString
                mock.doSomethingElse anyString
            }
        }

        when:
        mock.doSomething 1, 'Abc'
        mock.doSomething 2, 'xyz'
        mock.doSomethingElse 'test'

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 2, "xyz"'
    }

    def 'unexpected invocation for recorded strict expectation with maximum invocation count of zero'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomething anyInt, anyString
                times = 0
            }
        }

        when:
        mock.doSomething 1, 'Abc'

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 1, "Abc"'
    }

    def 'unexpected invocation for recorded expectation'() {
        given:
        new GroovyExpectations() {
            {
                mock.doSomething anyInt, anyString; times = 1
            }
        }

        when:
        mock.doSomething 1, 'Abc'
        mock.doSomething 2, 'xyz'

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 2, "xyz"'
    }

    def 'unexpected invocation for verified expectation'() {
        given:
        mock.doSomething 123, 'Test'
        mock.doSomethingElse 'abc'

        when:
        new GroovyVerifications() {
            {
                mock.doSomething withEqual(123), anyString
                times = 0
            }
        }

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 123, "Test"'
    }

    def 'unexpected invocation for expectations verified in order'() {
        given:
        mock.doSomethingElse 'test'
        mock.doSomething 123, 'Test'

        when:
        new GroovyVerificationsInOrder() {
            {
                mock.doSomethingElse anyString
                mock.doSomething anyInt, anyString; times = 0
            }
        }

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'with arguments: 123, "Test"'
    }

    def 'unexpected first invocation for expectations partially verified in order'() {
        given:
        mock.doSomething(-5, 'abc')
        mock.doSomethingElse 'test'
        mock.doSomething 123, 'Test'

        when:
        new GroovyVerificationsInOrder() {
            {
                mock.doSomethingElse anyString
                unverifiedInvocations()
                mock.doSomething anyInt, anyString
            }
        }

        then:
        UnexpectedInvocation e = thrown()
        e.toString().contains 'with arguments: "test"'
        e.cause.toString().contains 'with arguments: -5, "abc"'
    }

    def "unexpected last invocation for expectations partially verified in order"() {
        given:
        mock.doSomethingElse 'test'
        mock.doSomething 123, 'Test'
        mock.doSomething(-5, 'abc')

        when:
        new GroovyVerificationsInOrder() {
            {
                mock.doSomethingElse anyString
                unverifiedInvocations()
                mock.doSomething withEqual(123), anyString
            }
        }

        then:
        UnexpectedInvocation e = thrown()
        e.toString().contains 'with arguments: 123, "Test"'
        e.cause.toString().contains 'with arguments: -5, "abc"'
    }

    def 'unexpected invocation after all others'() {
        given:
        mock.doSomethingElse 'Not verified'
        mock.doSomething 1, 'anotherValue'
        mock.doSomethingElse 'test'

        and:
        final Verifications v = new GroovyVerifications() {
            {
                mock.doSomething anyInt, anyString
            }
        }

        when:
        new GroovyVerificationsInOrder() {
            {
                unverifiedInvocations()
                verifiedInvocations v
            }
        }

        then:
        UnexpectedInvocation e = thrown()
        e.toString().contains 'with arguments: 1, "anotherValue"'
        e.cause.toString().contains 'with arguments: "test"'
    }

    def 'unexpected invocation on method with no parameters'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomethingElse anyString
            }
        }

        when:
        mock.doSomething()

        then:
        UnexpectedInvocation e = thrown()
        e.message.contains 'doSomething()\n   on instance'
    }

    def 'missing invocation for recorded strict expectation'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomething anyInt, anyString
            }
        }

        when:
        Error e = RecordAndReplayExecution.endCurrentReplayIfAny()

        then:
        e instanceof MissingInvocation
        e.message.contains 'with arguments: any int, any String'
    }

    def 'missing invocation after recorded strict expectation which can occur one or more times'() {
        given:
        new GroovyStrictExpectations() {
            {
                mock.doSomethingElse anyString; maxTimes = -1
                mock.doSomething withEqual(1), anyString
            }
        }

        and:
        mock.doSomethingElse 'Test'

        when:
        Error e = RecordAndReplayExecution.endCurrentReplayIfAny()

        then:
        e instanceof MissingInvocation
        e.message.contains 'with arguments: 1, any String'

        cleanup:
        // satisfy the strict expectations so that the interceptor does not throw an exception
        TestRun.getRecordAndReplayForRunningTest().replayPhase.positionOnFirstStrictExpectation()
        mock.doSomethingElse ''
        mock.doSomething 1, ''
    }

    def 'missing invocation for recorded expectation'() {
        given:
        new GroovyExpectations() {
            {
                mock.doSomething anyInt, anyString; times = 2
            }
        }

        and:
        mock.doSomething 123, 'Abc'

        when:
        Error e = RecordAndReplayExecution.endCurrentReplayIfAny()

        then:
        e instanceof MissingInvocation
        e.message.contains 'with arguments: any int, any String'

        cleanup:
        mock.doSomething 1, ''
    }

    def 'missing invocation for verified expectation'() {
        when:
        new GroovyVerifications() {
            {
                mock.doSomething withEqual(123), anyString
            }
        }

        then:
        MissingInvocation e = thrown()
        e.message.contains 'with arguments: 123, any String'
    }

    def 'missing invocation for expectation verified in order'() {
        given:
        mock.doSomething 123, 'Test'

        when:
        new GroovyFullVerificationsInOrder() {
            {
                mock.doSomething anyInt, anyString
                minTimes = 3
            }
        }

        then:
        MissingInvocation e = thrown()
        e.message.contains 'with arguments: any int, any String'
    }

    def 'missing invocation for fully verified expectations'() {
        given:
        mock.doSomething 123, 'Abc'

        when:
        new GroovyFullVerifications() {
            {
                mock.doSomething anyInt, anyString
                times = 2
            }
        }

        then:
        MissingInvocation e = thrown()
        e.message.contains 'with arguments: any int, any String'
    }
}
