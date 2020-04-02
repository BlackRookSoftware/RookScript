RookScript (C) Black Rook Software 
==================================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in 1.3.0
----------------

- `Added` ZipFunctions: ZFITERATE
- `Added` FileSystemFunctions: FILENAMENOEXT
- `Added` ScriptValue: isInteger(), isFloat().
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
