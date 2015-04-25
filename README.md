Codebase for JMockit 1.x releases - [Documentation](http://jmockit.org) - [Release notes](http://jmockit.org/changes.html)

How to build the project:
* use JDK 1.8
* use Maven 3.2.5 or newer; there are four top-level modules:
    1. main/pom.xml            builds jmockit-1.n.jar, running JUnit and TestNG test suites
    2. coverage/pom.xml        builds jmockit-coverage-1.n.jar
    3. coverageTests/pom.xml   runs JUnit tests for the coverage tool
    4. samples/pom.xml         runs all sample test suites