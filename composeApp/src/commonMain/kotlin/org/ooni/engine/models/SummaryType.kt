package org.ooni.engine.models

enum class SummaryType {
    Simple, // total + failed count
    Anomaly, // total + failed + anomaly/success count
    Performance, // download/upload speed + video quality
}
