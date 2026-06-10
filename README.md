# Black Rook RookScript

Copyright (c) 2017-2025 Black Rook Software.  
[https://github.com/BlackRookSoftware/RookScript](https://github.com/BlackRookSoftware/RookScript)

[Latest Release](https://github.com/BlackRookSoftware/RookScript/releases/latest)  
[Online Javadoc](https://blackrooksoftware.github.io/RookScript/javadoc/Core)  
[Quick Guide](https://github.com/BlackRookSoftware/RookScript/blob/master/QUICK-GUIDE.md)


### Required Libraries

NONE


### Required Java Modules

[java.base](https://docs.oracle.com/javase/10/docs/api/java.base-summary.html)  


### Where to Get

* [Maven Central](https://central.sonatype.com/artifact/com.blackrooksoftware/rookscript)  
* [GitHub Releases](https://github.com/BlackRookSoftware/JSON/releases/latest)


### Introduction

This library compiles and runs RookScript, a non-host-specific scripting language.


### Why?

Lots of scripting languages that interface with Java want to "script Java," which opens your runtime
into a world of abuse-able security problems. RookScript is a flexible runtime language with
flexible host interfacing, allowing each script to be as limited or wide in scope as the implementor
wishes. 


### Library

Contained in this release is a series of classes that should be used for RookScript functions. 
The javadocs contain basic outlines of each package's contents.


### Compiling with Maven

To install/compile this library and make all artifacts with Apache Maven, type:

	mvn install

To compile this library, type:

	mvn compile

To make Maven-compatible JARs of this library, type:

	mvn jar:jar

To make Javadocs:

	mvn javadoc:javadoc

To run tests, type:

	mvn test

To generate a coverage report, type:

	mvn test jacoco:report

To clean up everything:

	mvn clean


### Some Examples

Hello World in RookScript.

```
entry main()
{
	println("Hello, world!");
}
```

Creating a quick script instance and calling `main()`. 

```Java
ScriptInstance instance = ScriptInstance.createBuilder()
	.withSource(new File(fileName))
	.withEnvironment(ScriptEnvironment.createStandardEnvironment())
	.withFunctionResolver(CommonFunctions.createResolver())
	.withScriptStack(16, 512)
	.createInstance();

Object result = instance.callAndReturnAs(Object.class, "main");
```

### Executor Tool

The class `com.blackrook.rookscript.tools.ScriptExecutor` contains a main file that can run
RookScript files like programs. By default, it expects a `main` entry point with a single parameter,
which is set to a list of the script arguments.

Call it with the `--help` switch to output help.


### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/RookScript/javadoc/Core](https://blackrooksoftware.github.io/RookScript/javadoc/Core)


### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
