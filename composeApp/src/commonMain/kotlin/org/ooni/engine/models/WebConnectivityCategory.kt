package org.ooni.engine.models

enum class WebConnectivityCategory(val code: String) {
    ALDR("ALDR"),
    REL("REL"),
    PORN("PORN"),
    PROV("PROV"),
    POLR("POLR"),
    HUMR("HUMR"),
    ENV("ENV"),
    MILX("MILX"),
    HATE("HATE"),
    NEWS("NEWS"),
    XED("XED"),
    PUBH("PUBH"),
    GMB("GMB"),
    ANON("ANON"),
    DATE("DATE"),
    GRP("GRP"),
    LGBT("LGBT"),
    FILE("FILE"),
    HACK("HACK"),
    COMT("COMT"),
    MMED("MMED"),
    HOST("HOST"),
    SRCH("SRCH"),
    GAME("GAME"),
    CULTR("CULTR"),
    ECON("ECON"),
    GOVT("GOVT"),
    COMM("COMM"),
    CTRL("CTRL"),
    IGO("IGO"),
    MISC("MISC"),
    ;

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code }
    }
}
