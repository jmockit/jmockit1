Codebase for JMockit 1.x releases - [Documentation](http://jmockit.github.io) - [Release notes](http://jmockit.github.io/changes.html)

How to build the project:
* use JDK 1.8 or newer
* use Maven 3.6.0 or newer; the following are the top-level modules:
    1. main/pom.xml: builds jmockit-1.n.jar, running JUnit 4 and TestNG test suites
    2. coverageTests/pom.xml: runs JUnit 4 tests for the coverage tool
    3. samples/pom.xml: various sample test suites (tutorial, LoginService, java8testing) using JUnit 4, 5, or TestNG 6
    4. samples/petclinic/pom.xml: integration testing example using Java EE 8