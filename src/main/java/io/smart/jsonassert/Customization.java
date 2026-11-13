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
    private final JsonValueCustomization customization;

    private Customization(String path, JsonValueCustomization customization) {
        if (customization == null) {
            throw new IllegalArgumentException("customization must not be null");
        }
        this.pattern = tokenize(path);
        this.customization = customization;
    }

    /** Binds a boolean matcher or a detailed comparator to a JSON path. */
    public static Customization customization(String path, JsonValueCustomization customization) {
        return new Customization(path, customization);
    }

    public JsonValueMatcher getMatcher() {
        return customization instanceof JsonValueMatcher matcher ? matcher : null;
    }

    /** Finds the first customization whose pattern matches the given actual path. */
    public static Optional<JsonValueMatcher> findMatcher(List<Customization> customizations, String actualPath) {
        List<String> actualTokens = tokenize(actualPath);
        for (Customization c : customizations) {
            if (c.customization instanceof JsonValueMatcher matcher && matches(c.pattern, actualTokens)) {
                return Optional.of(matcher);
            }
        }
        return Optional.empty();
    }

    static Optional<JsonValueComparator> findComparator(List<Customization> customizations, String actualPath) {
        List<String> actualTokens = tokenize(actualPath);
        for (Customization c : customizations) {
            if (c.customization instanceof JsonValueComparator comparator && matches(c.pattern, actualTokens)) {
                return Optional.of(comparator);
            }
        }
        return Optional.empty();
    }

    public static Optional<JsonValueCustomization> findCustomization(List<Customization> customizations, String actualPath) {
        List<String> actualTokens = tokenize(actualPath);
        for (Customization c : customizations) {
            if (matches(c.pattern, actualTokens)) {
                return Optional.of(c.customization);
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
        return "Customization" + pattern + " -> " + customization.getClass().getSimpleName();
    }
}
