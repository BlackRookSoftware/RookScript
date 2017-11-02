@echo off
REM %1: version %2: appendix
REM ===== Set Paths ====

set PROJECT_VERSION=%1
set PROJECT_APPENDIX=%2
set PROJECT_DATE=%date:~10,4%.%date:~4,2%.%date:~7,2%
set BUILD_SETTINGS=-Dbuild.version.number=%PROJECT_DATE%-%PROJECT_VERSION% -Dbuild.version.appendix=%PROJECT_APPENDIX%

call ant release %BUILD_SETTINGS% 1> build.log 2>&1
