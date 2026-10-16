package com.rukamini.mail.jpa.repository;

import com.rukamini.mail.jpa.entity.TUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * CWF-schema repository for {@link TUser} ({@code T_USER} table).
 *
 * <p>Used by {@link com.rukamini.mail.user.GraphUserDirectoryAdapter} as a
 * low-latency L1 lookup before falling through to the Microsoft Graph API.
 * The CWF database is on the same network segment as the application, making
 * it significantly faster than an outbound Graph REST call.
 */
@Repository
public interface TUserRepository extends JpaRepository<TUser, String> {

    /**
     * Looks up an active user by their SAM account name / login ID
     * (case-insensitive).
     *
     * <p>Filters out inactive users ({@code ACTIVE_IND = 'N'}) to mirror the
     * {@code accountEnabled eq true} predicate used in the Graph fallback.
     *
     * @param userId the SAM account name to search for
     * @return the matching {@link TUser}, or empty when not found / inactive
     */
    @Query(
            "SELECT u FROM TUser u WHERE UPPER(u.userId) = UPPER(:userId)"
                    + " AND u.activeInd <> 'N'")
    Optional<TUser> findActiveByUserId(@Param("userId") String userId);
}
