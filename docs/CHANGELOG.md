RookScript (C) Black Rook Software 
==================================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in 1.14.0
-----------------

- `Changed` Host function `EXEC()` will not auto-close any **non-file** stream, if used.


Changed in 1.13.0
-----------------

- `Added` Script.getScriptEntryNames().
- `Changed` Host function `EXEC()` will not auto-close any of the standard streams, if used.


Changed in 1.12.1.1
-------------------

- `Changed` Fixed some documentation.


Changed in 1.12.1
-----------------

- `Fixed` SUBSTR would produce an exception if `endIndex` were out-of-bounds, due to a typo.


Changed in 1.12.0
-----------------

- `Changed` Added a prettifying function to WRITEJSON and JSONSTR in JSONFunctions.


Changed in 1.11.0
-----------------

- `Added` Exposed/added some script parsing methods in ScriptParser.
- `Changed` Underlying error type in ScriptParser.
- `Changed` Constructors for ScriptParseException and multiple errors.
- `Changed` Return type for ScriptParser.parseScriptlet(Script).


Changed in 1.10.2
-----------------

- `Fixed` EXEC() uses the working directory correctly.


Changed in 1.10.1
-----------------

- `Fixed` COLOR() returned the wrong value.
- `Fixed` EXEC() was handled suboptimally.


Changed in 1.10.0
-----------------

- `Fixed` Preprocessor handling of backslashes.
- `Added` ScriptValue.listApply() functions. 
- `Changed` Better reflection with creation from lists and application to lists/arrays.
- `Changed` Some documentation for clarity.


Changed in 1.9.1
----------------

- `Changed` Added additional directory scanning parameters to FILELIST.


Changed in 1.9.0
----------------

- `Fixed` ScriptValue.mapApply(...) didn't apply map fields properly.
- `Changed` Reflection on Objects in RookScript looks up fields/methods case-insensitively.


Changed in 1.8.1
----------------

- `Fixed` Potential NPE via `#include` directive in preprocessor.
- `Fixed` Classpath resolution in preprocessor.


Changed in 1.8.0
----------------

- `Fixed / Changed` Scope handling was not fleshed out and also a little buggy.
- `Changed` Documentation for clarifying "truthiness" in the quick guide that wasn't... uh... true.


Changed in 1.7.4
----------------

- `Fixed` ObjectVariableResolver.isReadOnly() never returned a proper value.
- `Fixed` Some erroneous documentation.
- `Changed` CSWRITE / CSWRITELN now writes "null" if input is null.


Changed in 1.7.3
----------------

- `Fixed` `each` loops had left stack garbage, and affected other iterators if it was exited before the iterator was finished.


Changed in 1.7.2
----------------

- `Fixed` Strings of length > 0 should have been `true`, if converted to boolean.


Changed in 1.7.1
----------------

- `Fixed` Function parsing bug when amount of parameters were less than the required amount.


Changed in 1.7.0
----------------

- `Added` System functions.
- `Added` Added STRSPLIT().
- `Changed` Some function documentation.


Changed in 1.6.0
----------------

- `Added` JSON parsing/writing functions.


Changed in 1.5.1
----------------

- `Fixed` Doubling-up of function/entry labels in script if the name had mixed case.


Changed in 1.5.0
----------------

- `Added` SROPEN() and SWOPEN().
- `Added` STRREPLACEALL().
- `Added` Missing Date function doc output in ScriptExecutor.
- `Fixed` Markdown output for function docs was missing handling null Usage.
- `Changed` Swapped parameters in DATEFORMAT().


Changed in 1.4.4
----------------

- `Added` Markdown output option for printing function help in ScriptExecutor.
- `Changed` Some better error message wording.


Changed in 1.4.3
----------------

- `Added` SUBLIST().


Changed in 1.4.2
----------------

- `Changed` List / Map deref did not parse after expression values. Now they do.
- `Changed` The ScriptExecutor application will not attempt to pass command line arguments to an entry point without a parameter.


Changed in 1.4.1
----------------

- `Changed` Changed some function documentation.
- `Changed` Built-in function neatening, and reducing memory leak chances.
- `Changed` Built-in function READBYTE(): This does not return a DataUnderflow error.


Changed in 1.4.0
----------------

- `Added` Feature: Maps can now use numeric, boolean, and string literals as keys (they are converted to strings).
- `Changed` How AbstractVariableResolver are exported as strings (equivalent to map literals).


Changed in 1.3.1
----------------

- `Added` ScriptValue: isInteger(), isFloat().
- `Changed` Modified some more function documentation.


Changed in 1.3.0
----------------

- `Added` ZipFunctions: ZFITERATE
- `Added` FileSystemFunctions: FILENAMENOEXT
- `Changed` Modified some function documentation.


Changed in 1.2.2
----------------

- `Changed` Added some stuff to the script executor.


Changed in 1.2.1
----------------

- `Fixed` Some functions were not treated as case-insensitive.


Changed in 1.2.0
----------------

- `Fixed` Buffer bounds checks on all BUF* get/put functions.
- `Added` DigestFunctions


Changed in 1.1.0
----------------

- `Added` ScriptExecutor main class.
- `Added` Added GZISOPEN / GZOSOPEN to ZipFunctions.
- `Changed` Exception thrown in ScriptValue.createForType(...) if conversion absolutely fails.


Changed in 1.0.0
----------------

- Base release.
