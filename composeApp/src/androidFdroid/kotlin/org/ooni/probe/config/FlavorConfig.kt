package org.ooni.probe.config

class FlavorConfig : FlavorConfigInterface {
    override val optionalFeatures: Set<OptionalFeature>
        get() = emptySet()
}
