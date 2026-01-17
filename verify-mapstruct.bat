@echo off
REM MapStruct Generation Verification Script for Windows
REM Fails the build if MapStruct mapper implementations are not generated

set MAPSTRUCT_IMPL_PATH=target\generated-sources\annotations\org\planmoni\safehavenservice\mapper\SafeHavenMapperImpl.java

if not exist "%MAPSTRUCT_IMPL_PATH%" (
    echo ERROR: MapStruct implementation not found at %MAPSTRUCT_IMPL_PATH%
    echo MapStruct annotation processing failed. Build will fail.
    exit /b 1
)

echo SUCCESS: MapStruct implementation found at %MAPSTRUCT_IMPL_PATH%
exit /b 0
