#!/bin/bash
# MapStruct Generation Verification Script
# Fails the build if MapStruct mapper implementations are not generated

set -e

MAPSTRUCT_IMPL_PATH="target/generated-sources/annotations/org/planmoni/safehavenservice/mapper/SafeHavenMapperImpl.java"

if [ ! -f "$MAPSTRUCT_IMPL_PATH" ]; then
    echo "ERROR: MapStruct implementation not found at $MAPSTRUCT_IMPL_PATH"
    echo "MapStruct annotation processing failed. Build will fail."
    exit 1
fi

echo "SUCCESS: MapStruct implementation found at $MAPSTRUCT_IMPL_PATH"
exit 0
