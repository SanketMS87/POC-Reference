package com.rukamini.mail.user;

/**
 * Immutable user data transfer object returned by {@link UserDirectoryService}.
 *
 * <p>A Java 17 record — all fields are final; the compiler generates the
 * canonical constructor, accessors ({@code email()}, {@code firstName()},
 * {@code lastName()}), {@code equals}, {@code hashCode}, and {@code toString}
 * automatically.
 *
 * @param email     the user's corporate e-mail address (may be {@code null} if
 *                  the directory entry does not carry one)
 * @param firstName the user's given name
 * @param lastName  the user's family name
 */
public record UserDto(String email, String firstName, String lastName) {}
