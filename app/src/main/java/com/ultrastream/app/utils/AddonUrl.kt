package com.ultrastream.app.utils

fun buildAddonBaseUrl(addonUrl: String): String {
    var base = addonUrl
    if (base.endsWith("/manifest.json")) base = base.removeSuffix("/manifest.json")
    else if (base.endsWith("manifest.json")) base = base.removeSuffix("manifest.json")
    if (base.endsWith("/")) base = base.removeSuffix("/")
    return base
}
