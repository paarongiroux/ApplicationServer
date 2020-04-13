# ApplicationServer

To run this application: 
- start a new netbeans project.
- copy all of the contents of this repo into the root directory of that project.
- compile the netbeans project.
- move the PlusOne.class and PlusOneAux.class file into docRoot/appserver/job/impl
- in three terminal windows, cd into build/classes and run the following commands in this order: 

  `java web/SimpleWebServer ../../config/WebServer.properties`
  
  `java appserver/satellite/Satellite ../../config/Satellite.Earth.properties ../../config/WebServer.properties ../../config/Server.properties`
  
  `java appserver/client/PlusOneClient ../../config/Satellite.Earth.properties`
