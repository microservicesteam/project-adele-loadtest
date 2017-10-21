project-adele-loadtest
======================

Execute all test cases with the following command:

    $mvn gatling:test

In order to execute only one test case use the following command:

    $mvn gatling:test -Dgatling.simulationClass=adele.DefaultBookingSimulation
