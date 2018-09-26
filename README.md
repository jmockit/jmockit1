Codebase for JMockit 1.x releases - [Documentation](http://jmockit.github.io) - [Release notes](http://jmockit.github.io/changes.html)

How to build the project:
* use JDK 1.8 or newer
* use Maven 3.5.0 or newer; the following are the top-level modules:
    1. main/pom.xml: builds jmockit-1.n.jar, running JUnit and TestNG test suites
    2. coverageTests/pom.xml: runs JUnit tests for the coverage tool
    3. samples: various sample test suites