Codebase for JMockit 1.x releases - [Documentation](http://jmockit.org) - [Release notes](http://jmockit.org/changes.html)

How to build the project:
* use JDK 1.8
* use Maven 3.3.1 or newer; the following are the top-level modules:
    1. main/pom.xml            builds jmockit-1.n.jar, running JUnit and TestNG test suites
    2. coverageTests/pom.xml   runs JUnit tests for the coverage tool
    3. others in samples dir   various sample test suites