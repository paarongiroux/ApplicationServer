# ApplicationServer

To run this application: 
- start a new netbeans project.
- copy all of the contents of this repo into the root directory of that project.
- compile the netbeans project.
- move the PlusOne.class, PlusOneAux.class and Fibonacci.class files into docRoot/appserver/job/impl
- in six terminal windows, cd into build/classes and run the following commands in this order (each in its own window): 

  `java web/SimpleWebServer ../../config/WebServer.properties`
  
  `java appserver/server/Server /../../config/Server.properties`
  
  `java appserver/satellite/Satellite ../../config/Satellite.Earth.properties ../../config/WebServer.properties ../../config/Server.properties`
  
  `java appserver/satellite/Satellite ../../config/Satellite.Mercury.properties ../../config/WebServer.properties ../../config/Server.properties`
  
  `java appserver/satellite/Satellite ../../config/Satellite.Venus.properties ../../config/WebServer.properties ../../config/Server.properties`
  
  `java appserver/client/FibonacciClient`
