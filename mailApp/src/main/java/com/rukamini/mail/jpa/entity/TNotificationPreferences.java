package com.rukamini.mail.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "T_NOTIFICATION_PREFERENCES")
public class TNotificationPreferences {
    @Id
    @SequenceGenerator(
            name = "T_NOTIFICATION_PREF_ID_GENERATOR",
            sequenceName = "SEQ_T_NOTIFICATION_PREF",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "T_NOTIFICATION_PREF_ID_GENERATOR")
    @Column(name = "NOTIFICATION_PREF_ID")
    private long notePrefId;

    @Column(name = "USER_ID")
    private String userId;

    //	@ManyToOne
    //	@JoinColumn(name = "ATTRIBUTE_ID", insertable = true, updatable = true)
    @Column(name = "ATTRIBUTE_ID")
    private String attributeId;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_TS")
    private Date createdTs;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_TS")
    private Date updatedTs;

    @Column(name = "EMAIL")
    private String emailPref;

    @Column(name = "INTERNAL_NOTIFICATION")
    private String internalNotePref;

    @Column(name = "RECEIVE_NOTIFICATION")
    private String receiveNotification;
}
