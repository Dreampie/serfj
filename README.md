
SerfJ - Simplest Ever REST Framework for Java
=============================================
Using SerfJ is the easiest way of developing Java [Rest] web applications. It helps
you develop your application over an elegant MVC arquitecture, giving more importance
to convention than configuration, so for example, you will not have to have configuration 
files or annotations in order to specify which view serves a controller's method. However, 
SerfJ is a very flexible library, so if you want to jump over those conventions, you can 
configure the behaviour of your applications as you like.

The framework tries to meet [JSR311] specification, but it doesn't follow every point 
of that, because the purpose is to have a very intuitive library, and some some aspects 
of the specification are out of the scope of SerfJ.

For documentation and downloads, please visit the website: http://serfj.sourceforge.net

### Release notes
#### Version 0.3.4 (20120424)

* RestController.getRemoteAddress() doesn't check proxy headers.

#### Version 0.3.3 (20120420)

* Requests with null param values cause NPE.
* Binaries compiled for Java 5.

#### Version 0.3.2 (20120314)

* Updated Maven plugins and added versions at POM for plugins without version.
* Updated JAR dependencies versions
* Built on Java 6
* Extension .64 doesn't work. Now it's changed to .base64. Tests added.
* For resources called 'Signatures' the singular is not well made. Added an exception for signatures. Tests added.
* Identifiers for resources didn't allow alphabet chars although the
            identifier started with number. Changed to allow this kind of identifiers. Tests added.
* GroupId for serfj must be net.sf.serfj instead of net.sf. Changed to net.sf.serfj.
* When not serializer is found a NullPointerException is thrown.
* NPE thrown when server is not reached
* Fix Javadoc formatting

#### Version 0.3.1 (20110804)

* Calling getId(resource) always returns null
* Add method getId() to controllers
* RestServlet refactor

Copyright 2010 Eduardo Yáñez Parareda, licensed under the [ApacheLicense]
 
[ApacheLicense]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License, Version 2.0"
[Rest]: http://en.wikipedia.org/wiki/Representational_State_Transfer "REST"
[JSR311]: http://jcp.org/en/jsr/detail?id=311 "JSR-311"