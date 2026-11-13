package io.smart.jsonassert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Binds a {@link JsonValueMatcher} to a JSON path, mirroring JSONAssert's Customization.
 *
 * Paths use dot notation with bracket indices, e.g.:
 * <ul>
 *   <li>{@code user.name}</li>
 *   <li>{@code team.members[0].role}</li>
 * </ul>
 * The segment {@code *} acts as a single-segment wildcard matching any key or
 * array index, e.g. {@code team.members[*].role} applies to every member.
 */
public final class Customization {

    private final List<String> pattern;
    private final JsonValueMatcher matcher;
    private final JsonValueComparator comparator;

    private Customization(String path, JsonValueMatcher matcher) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher must not be null");
        }
        this.pattern = tokenize(path);
        this.matcher = matcher;
        this.comparator = null;
    }

    private Customization(String path, JsonValueComparator comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }
        this.pattern = tokenize(path);
        this.matcher = null;
        this.comparator = comparator;
    }

    public static Customization customization(String path, JsonValueMatcher matcher) {
        return new Customization(path, matcher);
    }

    /** Binds a detailed, recursive comparator to a JSON path. */
    public static Customization customization(String path, JsonValueComparator comparator) {
        return new Customization(path, comparator);
    }

    public JsonValueMatcher getMatcher() {
        return matcher;
    }

    /** Finds the first customization whose pattern matches the given actual path. */
    public static Optional<JsonValueMatcher> findMatcher(List<Customization> customizations, String actualPath) {
        List<String> actualTokens = tokenize(actualPath);
        for (Customization c : customizations) {
            if (c.comparator == null && matches(c.pattern, actualTokens)) {
                return Optional.of(c.matcher);
            }
        }
        return Optional.empty();
    }

    static Optional<JsonValueComparator> findComparator(List<Customization> customizations, String actualPath) {
        List<String> actualTokens = tokenize(actualPath);
        for (Customization c : customizations) {
            if (c.comparator != null && matches(c.pattern, actualTokens)) {
                return Optional.of(c.comparator);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(List<String> patternTokens, List<String> actualTokens) {
        if (patternTokens.size() != actualTokens.size()) {
            return false;
        }
        for (int i = 0; i < patternTokens.size(); i++) {
            String p = patternTokens.get(i);
            if (!"*".equals(p) && !p.equals(actualTokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts "team.members[0].skills" into tokens [team, members, 0, skills].
     */
    static List<String> tokenize(String path) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String segment : path.replace("[", ".[").replace("]", "").split("\\.")) {
            if (!segment.isEmpty()) {
                tokens.add(segment.replace("[", ""));
            }
        }
        return Collections.unmodifiableList(tokens);
    }

    @Override
    public String toString() {
        return "Customization" + pattern + " -> "
                + (matcher != null ? matcher.getClass().getSimpleName() : comparator.getClass().getSimpleName());
    }
}
