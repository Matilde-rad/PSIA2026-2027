package pt.ulisboa.tecnico.psia.g2.movies;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MovieLensCsv {

    private static final Pattern YEAR = Pattern.compile("\\s*\\((\\d{4})\\)\\s*$");
    private static final Pattern ALT_TITLE = Pattern.compile("\\s*\\([^()]*\\)\\s*$");
    private static final Pattern TRAILING_ARTICLE =
            Pattern.compile("^(.+), (The|A|An|La|Le|Les|El|Il|Die|Der|Das)$");
    private static final String NO_GENRES = "(no genres listed)";

    private MovieLensCsv() {
    }

    public record MovieRow(int id, String title, Integer year, List<String> genres) {
    }

    public static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    public static Integer parseYear(String title) {
        Matcher m = YEAR.matcher(title);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    public static List<String> parseGenres(String raw) {
        if (NO_GENRES.equals(raw)) {
            return List.of();
        }
        return List.of(raw.split("\\|"));
    }

    public static MovieRow parseMovieLine(String line) {
        List<String> fields = parseCsvLine(line);
        if (fields.size() != 3) {
            throw new IllegalArgumentException("Expected 3 fields in movies.csv line: " + line);
        }
        int id;
        try {
            id = Integer.parseInt(fields.get(0).strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Non-integer movie id in movies.csv line: " + line, e);
        }
        String title = fields.get(1).strip();
        return new MovieRow(id, title, parseYear(title), parseGenres(fields.get(2)));
    }

    public static String titleKey(String title) {
        if (title == null) {
            return "";
        }
        String s = YEAR.matcher(title.strip()).replaceFirst("").strip();
        String withoutAlt = ALT_TITLE.matcher(s).replaceFirst("").strip();
        if (!withoutAlt.isEmpty()) {
            s = withoutAlt;
        }
        Matcher article = TRAILING_ARTICLE.matcher(s);
        if (article.matches()) {
            s = article.group(2) + " " + article.group(1);
        }
        return s.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }
}
