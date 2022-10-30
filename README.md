Codebase for JMockit 1.x releases - [Documentation](http://jmockit.github.io) - [Release notes](http://jmockit.github.io/changes.html)

How to build the project:
* use JDK 1.8 or newer
* use Maven 3.6.0 or newer; the following are the top-level modules:
    1. main/pom.xml: builds jmockit-1.n.jar, running JUnit 4 and TestNG test suites
    2. coverageTests/pom.xml: runs JUnit 4 tests for the coverage tool
    3. samples/pom.xml: various sample test suites (tutorial, LoginService, java8testing) using JUnit 4, 5, or TestNG 6
    4. samples/petclinic/pom.xml: integration testing example using Java EE 8

This fork contains pull requests from main repo as well as updated libraries within build.

  - [665](https://github.com/jmockit/jmockit1/pull/665) from fork [vimil](https://github.com/vimil/jmockit1) condy arrayindexoutofboundsexception fix
  - [695](https://github.com/jmockit/jmockit1/pull/695) from fork [don-vip](https://github.com/don-vip/jmockit1) Fix NPE when className is null
  - [697](https://github.com/jmockit/jmockit1/pull/697) from fork [Saljack](https://github.com/Saljack/jmockit1) Fix Tested fullyInitialized instance with interfaces in constructor
  - [712](https://github.com/jmockit/jmockit1/pull/712) from fork [Saljack](https://github.com/Saljack/jmockit1) Add method name check for generic methods Expectations
  - [734](https://github.com/jmockit/jmockit1/pull/734) from fork [tsmock](https://github.com/tsmock/jmockit1) Mocks created by JUnit4 tests are not cleaned up when run with JUnit5
  - [736](https://github.com/jmockit/jmockit1/pull/736) from fork [Col-E](https://github.com/Col-E/jmockit1) Add suport for Java 11+ based off this repo
  - [68](https://github.com/hazendaz/jmockit1/pull/68) from fork [Col-E](https://github.com/Col-E/jmockit1) after sync up from PR 736.

Considerations

  - Testing confirmed to work through jdk 17
  - Github Actions fail with testng and applet testing
  - New launcher pom in root to build entire project (extra modules rely on released copy of fork currently)

Releasing

  - Use jdk 8 as javadocs fail on anything newer, various attempts to update were preformed but javadocs is not giving any specific reason.