# Black Rook RookScript

Copyright (c) 2017-2019 Black Rook Software. All rights reserved.  
[http://blackrooksoftware.com/projects.htm?name=rookscript](http://blackrooksoftware.com/projects.htm?name=rookscript)  
[https://github.com/BlackRookSoftware/RookScript](https://github.com/BlackRookSoftware/RookScript)

### NOTICE

This library is currently in **EXPERIMENTAL** status. This library's API
may change many times in different ways over the course of its development!

### Required Libraries

NONE

### Required Java Modules

[java.base](https://docs.oracle.com/javase/10/docs/api/java.base-summary.html)  

### Introduction

This library assists in compiling/running RookScript, a non-host-specific scripting language.

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
	
### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
