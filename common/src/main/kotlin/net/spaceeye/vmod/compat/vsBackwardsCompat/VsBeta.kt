package org.valkyrienskies.core.api

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an experimental API, it may be changed or removed without warning in future versions of " +
            "Valkyrien Skies. Addons that use this in production code are recommended to catch a potential " +
            "LinkageError or NotImplementedError and recover gracefully if this API throws or is not available."
)
annotation class VsBeta
