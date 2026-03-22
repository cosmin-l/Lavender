package org.cl.lavender;

import java.awt.Color;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {

    public static final List<Theme> THEMES = List.of(

        new Theme("Midnight",
            new Color(30, 30, 30),    new Color(212, 212, 212),
            new Color(43, 43, 43),    new Color(110, 110, 110),
            new Color(43, 43, 43),    new Color(150, 150, 150),
            new Color(255, 255, 255, 25), new Color(200, 200, 200, 60),
            new Color(80, 70, 20),    new Color(200, 160, 0),
            new Color(60, 60, 60),    new Color(200, 80, 80)),

        new Theme("Dracula",
            new Color(40, 42, 54),    new Color(248, 248, 242),
            new Color(33, 34, 44),    new Color(98, 114, 164),
            new Color(33, 34, 44),    new Color(98, 114, 164),
            new Color(255, 255, 255, 20), new Color(189, 147, 249, 60),
            new Color(80, 54, 20),    new Color(255, 184, 108),
            new Color(68, 71, 90),    new Color(255, 85, 85)),

        new Theme("Monokai",
            new Color(39, 40, 34),    new Color(248, 248, 242),
            new Color(44, 45, 38),    new Color(117, 113, 94),
            new Color(39, 40, 34),    new Color(117, 113, 94),
            new Color(255, 255, 255, 20), new Color(230, 219, 116, 60),
            new Color(80, 75, 10),    new Color(230, 219, 116),
            new Color(63, 64, 55),    new Color(249, 38, 114)),

        new Theme("Nord",
            new Color(46, 52, 64),    new Color(216, 222, 233),
            new Color(59, 66, 82),    new Color(76, 86, 106),
            new Color(46, 52, 64),    new Color(76, 86, 106),
            new Color(255, 255, 255, 20), new Color(136, 192, 208, 60),
            new Color(40, 56, 80),    new Color(136, 192, 208),
            new Color(67, 76, 94),    new Color(191, 97, 106)),

        new Theme("Gruvbox",
            new Color(40, 40, 40),    new Color(235, 219, 178),
            new Color(50, 48, 47),    new Color(124, 111, 100),
            new Color(40, 40, 40),    new Color(124, 111, 100),
            new Color(255, 255, 255, 20), new Color(184, 187, 38, 60),
            new Color(60, 54, 10),    new Color(184, 187, 38),
            new Color(80, 73, 69),    new Color(204, 36, 29)),

        new Theme("One Dark",
            new Color(40, 44, 52),    new Color(171, 178, 191),
            new Color(33, 37, 43),    new Color(91, 97, 110),
            new Color(33, 37, 43),    new Color(91, 97, 110),
            new Color(255, 255, 255, 20), new Color(97, 175, 239, 60),
            new Color(30, 50, 80),    new Color(97, 175, 239),
            new Color(53, 59, 69),    new Color(224, 108, 117)),

        new Theme("Tokyo Night",
            new Color(26, 27, 38),    new Color(169, 177, 214),
            new Color(22, 22, 30),    new Color(65, 72, 104),
            new Color(22, 22, 30),    new Color(65, 72, 104),
            new Color(255, 255, 255, 15), new Color(122, 162, 247, 60),
            new Color(30, 40, 80),    new Color(122, 162, 247),
            new Color(41, 46, 66),    new Color(247, 118, 142)),

        new Theme("Solarized Dark",
            new Color(0, 43, 54),     new Color(131, 148, 150),
            new Color(7, 54, 66),     new Color(88, 110, 117),
            new Color(0, 43, 54),     new Color(88, 110, 117),
            new Color(255, 255, 255, 20), new Color(181, 137, 0, 60),
            new Color(60, 50, 0),     new Color(181, 137, 0),
            new Color(7, 54, 66),     new Color(220, 50, 47)),

        new Theme("Abyss",
            new Color(14, 24, 39),    new Color(108, 158, 196),
            new Color(10, 18, 30),    new Color(40, 66, 99),
            new Color(10, 18, 30),    new Color(40, 66, 99),
            new Color(255, 255, 255, 15), new Color(0, 122, 204, 60),
            new Color(10, 30, 80),    new Color(0, 122, 204),
            new Color(30, 43, 57),    new Color(200, 50, 50)),

        new Theme("Obsidian",
            new Color(34, 34, 34),    new Color(163, 197, 109),
            new Color(28, 28, 28),    new Color(77, 117, 77),
            new Color(28, 28, 28),    new Color(77, 117, 77),
            new Color(255, 255, 255, 20), new Color(163, 197, 109, 60),
            new Color(40, 60, 20),    new Color(163, 197, 109),
            new Color(52, 52, 52),    new Color(200, 80, 80))
    );

    private static final Preferences PREFS = Preferences.userRoot().node("org/cl/lavender");

    private static Theme current = THEMES.get(0);

    public static Theme current() { return current; }

    public static void set(Theme theme) { current = theme; }

    public static Theme loadTheme() {
        String name = PREFS.get("theme", "Midnight");
        return THEMES.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElse(THEMES.get(0));
    }

    public static void saveTheme() {
        PREFS.put("theme", current.name());
    }
}
