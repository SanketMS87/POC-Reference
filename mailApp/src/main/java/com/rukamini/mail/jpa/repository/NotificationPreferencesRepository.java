package com.rukamini.mail.jpa.repository;

import com.rukamini.mail.jpa.entity.TNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferencesRepository
        extends JpaRepository<TNotificationPreferences, Long> {

    @Query(
            value =
                    "SELECT tn.USER_ID from {h-schema}T_NOTIFICATION_PREFERENCES tn "
                            + "INNER JOIN {h-schema}T_USER tu on tn.USER_ID = tu.USER_ID "
                            + "where UPPER(tn.USER_ID)=:userId and tn.ATTRIBUTE_ID=:attributeId"
                            + " and tn.EMAIL <> 'N' and tu.ACTIVE_IND <> 'N' LIMIT 1",
            nativeQuery = true)
    String fetchUserToSendMails(
            @Param("userId") String userId, @Param("attributeId") String attributeId);
}
