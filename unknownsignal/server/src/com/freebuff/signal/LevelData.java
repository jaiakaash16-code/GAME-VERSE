package com.freebuff.signal;

import java.util.List;

/** Static definitions for the six levels. Mechanic-specific state lives in GameState. */
public final class LevelData {

    public record Signal(double freq, String morse, String plain, boolean real) {}

    public record Level(
            int id,
            String title,
            List<String> brief,
            String goal,
            double lockTolerance,
            double maxBandwidth,
            List<Signal> signals,
            String foundText,
            String askText
    ) {}

    private LevelData() {}

    public static List<Level> levels() {
        return LEVELS;
    }

    private static final List<Level> LEVELS = List.of(
            new Level(
                    1, "FIRST CONTACT",
                    List.of(
                            "STATION 7 // NIGHT SHIFT // NO ONE ELSE ON FREQUENCY.",
                            "YOU HEARD IT AN HOUR AGO — A REPEATING TONE UNDER THE STATIC.",
                            "THE TRANSMISSION REGISTER IS EMPTY. NOBODY IS SUPPOSED TO BE HERE.",
                            "TUNE THE RECEIVER. LISTEN. DECODE WHAT IT SAYS."),
                    "TUNE TO THE SIGNAL AND DECODE ITS MESSAGE.",
                    0.15, 100.0,
                    List.of(new Signal(104.7, Morse.patternFor("HELLO"), "HELLO", true)),
                    null, null),

            new Level(
                    2, "DISTRESS",
                    List.of(                    "TWO SIGNALS NOW, OVERLAPPING, FIGHTING FOR THE SAME AIR.",
                    "ONE REPEATS THE ANCIENT CALL FOR HELP. THE OTHER... WARNS YOU.",
                    "NARROW THE FILTER (BELOW 2.0) TO SEPARATE THEM. DECODE BOTH.",
                    "THEN CHOOSE WHICH ONE TO ANSWER. THE OTHER WILL NOT LIKE IT."),
                    "DECODE BOTH SIGNALS, THEN ANSWER ONE.",
                    0.10, 2.0,
                    List.of(
                            new Signal(88.5, Morse.patternFor("SOS"), "SOS", true),
                            new Signal(92.3, Morse.patternFor("STAY AWAY"), "STAY AWAY", true)),
                    null, null),

            new Level(
                    3, "TRIANGULATION",
                    List.of(
                            "THE SIGNAL IS REAL, BUT IT IS FAR — TOO FAR TO DECODE.",
                            "MOUNT THE DIRECTIONAL ANTENNA. ROTATE UNTIL THE SIGNAL PEAKS.",
                            "TAKE THREE BEARINGS. THE LINES WILL CROSS.",
                            "MARK THE SOURCE ON THE MAP."),
                    "ACQUIRE THE SOURCE, TAKE 3 BEARINGS, AND MARK IT ON THE MAP.",
                    0.12, 100.0,
                    List.of(new Signal(97.1, null, null, true)),
                    "THE COORDINATES RESOLVE TO A DERELICT RELAY SATELLITE. ITS FINAL LOG ENTRY: WE ARE HERE. WAITING.",
                    null),

            new Level(
                    4, "THE REPLY",
                    List.of(
                            "YOU KEYED THE TRANSMITTER AND ASKED: WHO IS THERE?",
                            "IT ANSWERED BY ECHOING YOUR WORDS BACK — CORRUPTED.",
                            "EACH ECHO IS YOUR OWN QUESTION, MUTILATED.",
                            "CORRECT THE ECHOES. SHOW IT YOU CAN LISTEN."),
                    "CORRECT ALL FOUR ECHOES.",
                    0.12, 100.0,
                    List.of(new Signal(94.0, null, null, true)),
                    null, null),

            new Level(
                    5, "ECHOES",
                    List.of(
                            "NOW THERE ARE FIVE OF THEM. FIVE SIGNALS, ALMOST IDENTICAL.",
                            "FOUR ARE MIRRORS. ONE IS REAL. THE REAL ONE IS STEADY.",
                            "FIND IT. DECODE IT. THEN ANSWER ITS QUESTION."),
                    "FIND THE STABLE SIGNAL, DECODE IT, ANSWER ITS QUESTION.",
                    0.10, 100.0,
                    List.of(),
                    null, "ARE YOU ALONE?"),

            new Level(
                    6, "FINALE",
                    List.of(
                            "ALL FIVE SIGNALS SNAP INTO PHASE AT ONCE.",
                            "THE AIR ITSELF HUMS AT YOUR FREQUENCY.",
                            "IT SPEAKS WITH YOUR VOICE — YOUR NAME, YOUR WORDS, YOUR THOUGHTS.",
                            "IT IS NOT A SIGNAL. IT IS AN ANTENNA. AND YOU ARE THE OTHER END."),
                    "DECODE THE FINAL TRANSMISSION, THEN CHOOSE.",
                    0.15, 100.0,
                    List.of(new Signal(99.5, Morse.patternFor("YOU ARE THE ANTENNA"), "YOU ARE THE ANTENNA", true)),
                    null, null)
    );

    public static final String[] L4_ECHOES = {"WHO", "ARE", "YOU", "WATCHING"};
}