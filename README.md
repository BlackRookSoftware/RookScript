# Black Rook RookScript

Copyright (c) 2017-2020 Black Rook Software. All rights reserved.  
[https://github.com/BlackRookSoftware/RookScript](https://github.com/BlackRookSoftware/RookScript)

[Latest Release](https://github.com/BlackRookSoftware/RookScript/releases/latest)  
[Online Javadoc](https://blackrooksoftware.github.io/RookScript/javadoc/)  
[Quick Guide](https://github.com/BlackRookSoftware/RookScript/blob/master/QUICK-GUIDE.md)


### NOTICE

This library's API may change many times in different ways over the course of its development!


### Required Libraries

NONE


### Required Java Modules

[java.base](https://docs.oracle.com/javase/10/docs/api/java.base-summary.html)  


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


### Compiling with Ant

To compile this library with Apache Ant, type:

	ant compile

To make Maven-compatible JARs of this library (placed in the *build/jar* directory), type:

	ant jar

To make Javadocs (placed in the *build/docs* directory):

	ant javadoc

To compile main and test code and run tests (if any):

	ant test

To make Zip archives of everything (main src/resources, bin, javadocs, placed in the *build/zip* directory):

	ant zip

To compile, JAR, test, and Zip up everything:

	ant release

To clean up everything:

	ant clean


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
RookScript files like programs. By default, they expect a `main` entry point with a single parameter,
which is set to a list of the script arguments.

Call it with the `--help` switch to output help.


### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/RookScript/javadoc/](https://blackrooksoftware.github.io/RookScript/javadoc/)


### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
