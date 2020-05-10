RookScript (C) Black Rook Software 
==================================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in [NOW]
----------------

- `Added` System functions.


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
