package io.smart.jsonassert;

/** Callback accepted by {@link Customization}; returns a Boolean or a detailed result. */
@FunctionalInterface
public interface JsonValueCustomization {
    Object compare(Object expected, Object actual);
}
