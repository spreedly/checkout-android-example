# Migration Guide: v0.x to v1.0.0

## Overview

Spreedly Android Checkout SDK v1.0.0 is a stability milestone, not a breaking
change release. All public APIs from v0.14.x are preserved without modification.
The major version bump signals that the API surface is now stable and covered by
semantic versioning guarantees.

## Prerequisites

- Kotlin **2.3.10**
- Android Gradle Plugin **8.13.2**
- Compose BOM **2025.10.01**
- Minimum Android SDK: **26**
- Gradle: **8.14.3**

## Breaking Changes

None. v1.0.0 introduces no breaking API changes from v0.14.x.

## Step-by-Step Migration

1. Update your dependency version from `0.14.x` to `1.0.0`:

```kotlin
// build.gradle.kts
val spreedlyVersion = "1.0.0"

dependencies {
    implementation("com.spreedly:checkout-payments-core:$spreedlyVersion")
    implementation("com.spreedly:checkout-hostedfields:$spreedlyVersion")
    implementation("com.spreedly:checkout-paymentsheet:$spreedlyVersion")
    // add other modules as needed
}
```

2. Build and verify -- no code changes should be required.

## What Changed

- Published AARs now embed `META-INF/LICENSE` (Apache 2.0) inside `classes.jar`
- Maven distribution repository tags are GPG-signed
- CI pipeline hardened with runner stability guards and DAST scanning

## Support

The 0.x line enters maintenance and will receive security and crash fixes for
12 months (until 2027-04-30). If you encounter issues during migration, please
create an issue in the repository.
