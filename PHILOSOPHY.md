# Library Philosophy

This file is for answering questions around some of this library's design. This can also
serve as a reference point for why certain parts of the library are packaged a certain
way, or explaining choices made during creation, so that it can guide future decisions.

And if, down the line, a better approach is proposed, at least there's a paper trail for
the decisions already made so that more *educated* decisions can be made.


### What is the primary goal of this project?

To create a versatile scripting language and scripting system that is suitable for use from 
novice to adept developers. It must be suitable for both real-time and on-demand workloads.  
As a result, this library makes heavy use of techniques that try to minimize use of the GC, so that
real-time processes that call scripts at an interval do not cause many collections.

This can always be improved on.


### A lot of built-in functions are object-first. Why?

Java is an object-oriented language, and RookScript's function design, while *imperative* for
the most part, puts the object operated as the first parameter and returns that object on success
when a function would otherwise return nothing. This allows most functions to utilize the
partial operator (`->`) for function chaining on the same value/object for the same associated 
functions.

Users are encouraged to follow this pattern in order to keep a consistent programming flow.

