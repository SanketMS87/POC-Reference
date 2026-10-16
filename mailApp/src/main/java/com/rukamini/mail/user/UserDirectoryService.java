package com.rukamini.mail.user;

/**
 * Abstraction over the user directory used to resolve user identifiers to
 * e-mail addresses and display names.
 *
 * <p>Two implementations are provided and selected via
 * {@code @ConditionalOnProperty} — exactly one bean is registered per
 * deployment:
 *
 * <ul>
 *   <li>{@link IdamUserDirectoryAdapter} — backed by the Oracle IDAM schema
 *       ({@code V_USERS} view); active when
 *       {@code app.user-directory.provider=idam} (the default).
 *   <li>{@link GraphUserDirectoryAdapter} — backed by Microsoft Graph
 *       {@code /v1.0/users}; active when
 *       {@code app.user-directory.provider=graph}.
 * </ul>
 *
 * <p>Switching providers requires only changing the
 * {@code app.user-directory.provider} property in the relevant Spring profile
 * YAML ({@code application-{env}.yml}).  No code changes are needed.
 */
public interface UserDirectoryService {

    /**
     * Looks up a user by their SAM account name / login ID or full display
     * name ({@code "FirstName LastName"}).
     *
     * <p>Matching is case-insensitive.  Only active / enabled users are
     * returned; disabled accounts resolve to {@code null}.
     *
     * @param userId the SAM account name, login ID, or
     *               {@code "FirstName LastName"} string to search for
     * @return the resolved {@link UserDto}, or {@code null} when no enabled
     *         user is found
     */
    UserDto lookupByUserId(String userId);
}
