package org.ooni.probe.data.models

enum class Animation(
    val fileName: String,
) {
    QuizCorrect("checkMark"),
    QuizIncorrect("crossMark"),
    Circumvention("circumvention"),
    Experimental("experimental"),
    InstantMessaging("instant_messaging"),
    Performance("performance"),
    Websites("websites"),
    ;

    companion object {
        fun fromFileName(fileName: String) = entries.firstOrNull { animation -> fileName.contains(animation.fileName) }
    }
}
