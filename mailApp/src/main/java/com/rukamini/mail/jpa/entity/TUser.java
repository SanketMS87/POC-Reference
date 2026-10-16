/**
 * Copyright (C) 2014 Swiss Re. All rights reserved. This software is the
 * confidential and proprietary information of Swiss Re ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Swiss Re.
 */
package com.rukamini.mail.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

/*
 *
 * The persistent class for the T_USER database table.
 *
 */
@Entity
@Table(name = "T_USER")
public class TUser {

    @Id
    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "EMAIL")
    private String emailAddress;

    @Column(name = "ACTIVE_IND")
    private String activeInd;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Temporal(TemporalType.DATE)
    @Column(name = "CREATED_TS")
    private Date createdTs;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "UPDATED_TS")
    private Date updatedTs;

    @Column(name = "UPDTAED_BY")
    private String updtaedBy;

    @Column(name = "EMAIL_NOTIFICATION_PREF_IND")
    private String emailPreferenceInd;

    @Column(name = "REMINDER_PREF_IND")
    private String reminderPreferenceInd;

    @Column(name = "DEFAULT_DETACH_TAB")
    private String defaultDetachTab;

    @Column(name = "EMAIL_NOTIFICATION_IND_UPDT_DT")
    private Date generalIndUpdatesTS;

    @Column(name = "RMNDR_NOTIFICATION_IND_UPDT_DT")
    private Date reminderIndUpdatedTS;

    @Column(name = "TACIT_NOTIFICATION_IND")
    private String tacitNotificationInd;

    @Column(name = "NON_TACIT_NOTIFICATION_IND")
    private String nonTacitNotificationInd;

    @Column(name = "TACIT_TASK_NOTIFICATION_IND")
    private String tacitTaskNotificationInd;

    @Column(name = "TACIT_NOTI_IND_UPDT_DT")
    private Date tacitNotiIndUpdatedTS;

    @Column(name = "NON_TACIT_NOTI_IND_UPDT_DT")
    private Date nonTacitNotiIndUpdatedTS;

    @Column(name = "TACIT_TASK_NOTI_IND_UPDT_DT")
    private Date tacitTaskNotiIndUpdatedTS;

    @Column(name = "TACIT_AUTOMATION_IND")
    private String tacitAutomationInd;

    @Column(name = "TACIT_AUTO_IND_UPDT_DT")
    private Date tacitAutoIndUpdatedTS;

    @Column(name = "TACIT_NO_OF_DAYS")
    private String tacitNoOfDays;

    @Column(name = "NON_TACIT_NO_OF_DAYS")
    private String nonTacitNoOfDays;

    @Column(name = "TACIT_TASK_NO_OF_DAYS")
    private String tacitTaskNoOfDays;

    @Column(name = "TACIT_AUTO_NO_OF_DAYS")
    private String tacitAutoNoOfDays;

    @Column(name = "MAIN_BUSINESS_AREA")
    private String mainBusinessArea;

    @Column(name = "DEFAULT_BUS_AREA_IND")
    private String defaultBusAreaInd;

    @Column(name = "MODIFY_EXISTING_BUS_IND")
    private String modifyExtBusInd;

    @Column(name = "DEFAULT_BUS_AREA_UPD_TS")
    private Date defaultBusAreaUpdTs;

    @Column(name = "MODIFY_EXISTING_BUS_UPD_TS")
    private Date modifyExtBusUpdTs;

    @Column(name = "DEFAULT_ROLE")
    private String defaultRole;

    @Column(name = "REMINDER_NOTIFICATION_IND")
    private String remainderNoteInd;

    @Column(name = "USER_GROUPS_UPDATED_TS")
    private Date userGroupsUpdtTS;

    public Date getUserGroupsUpdtTS() {
        /*
         * Date returnDate=null; if(userGroupsUpdtTS!=null){ returnDate = new
         * Date(userGroupsUpdtTS.getTime()); } return returnDate;
         */
        return userGroupsUpdtTS;
    }

    public void setUserGroupsUpdtTS(Date userGroupsUpdtTS) {
        /*
         * if(userGroupsUpdtTS!=null){ this.userGroupsUpdtTS = new
         * Date(userGroupsUpdtTS.getTime()); }
         */
        this.userGroupsUpdtTS = userGroupsUpdtTS;
    }

    public String getRemainderNoteInd() {
        return remainderNoteInd;
    }

    public void setRemainderNoteInd(String remainderNoteInd) {
        this.remainderNoteInd = remainderNoteInd;
    }

    public Date getGeneralIndUpdatesTS() {
        /*
         * Date returnDate=null; if(generalIndUpdatesTS!=null){ returnDate = new
         * Date(generalIndUpdatesTS.getTime()); } return returnDate;
         */
        return generalIndUpdatesTS;
    }

    public void setGeneralIndUpdatesTS(Date generalIndUpdatesTS) {
        /*
         * if(generalIndUpdatesTS!=null){ this.generalIndUpdatesTS = new
         * Date(generalIndUpdatesTS.getTime()); }
         */
        this.generalIndUpdatesTS = generalIndUpdatesTS;
    }

    public Date getReminderIndUpdatedTS() {
        Date returnDate = null;
        if (reminderIndUpdatedTS != null) {
            returnDate = new Date(reminderIndUpdatedTS.getTime());
        }
        return returnDate;
    }

    public void setReminderIndUpdatedTS(Date reminderIndUpdatedTS) {
        if (reminderIndUpdatedTS != null) {
            this.reminderIndUpdatedTS = new Date(reminderIndUpdatedTS.getTime());
        }
    }

    public String getReminderPreferenceInd() {
        return reminderPreferenceInd;
    }

    public void setReminderPreferenceInd(String reminderPreferenceInd) {
        this.reminderPreferenceInd = reminderPreferenceInd;
    }

    public TUser() {}

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailPreferenceInd() {
        return emailPreferenceInd;
    }

    public void setEmailPreferenceInd(String emailPreferenceInd) {
        this.emailPreferenceInd = emailPreferenceInd;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActiveInd() {
        return this.activeInd;
    }

    public void setActiveInd(String activeInd) {
        this.activeInd = activeInd;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedTs() {
        Date returnDate = null;
        if (createdTs != null) {
            returnDate = new Date(createdTs.getTime());
        }
        return returnDate;
    }

    public void setCreatedTs(Date createdTs) {
        if (createdTs != null) {
            this.createdTs = new Date(createdTs.getTime());
        }
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getUpdatedTs() {
        Date returnDate = null;
        if (updatedTs != null) {
            returnDate = new Date(updatedTs.getTime());
        }
        return returnDate;
    }

    public void setUpdatedTs(Date updatedTs) {
        if (updatedTs != null) {
            this.updatedTs = new Date(updatedTs.getTime());
        }
    }

    public String getUpdtaedBy() {
        return this.updtaedBy;
    }

    public void setUpdtaedBy(String updtaedBy) {
        this.updtaedBy = updtaedBy;
    }

    public String getTacitAutomationInd() {
        return tacitAutomationInd;
    }

    public void setTacitAutomationInd(String tacitAutomationInd) {
        this.tacitAutomationInd = tacitAutomationInd;
    }

    public Date getTacitAutoIndUpdatedTS() {
        return tacitAutoIndUpdatedTS;
    }

    public void setTacitAutoIndUpdatedTS(Date tacitAutoIndUpdatedTS) {
        this.tacitAutoIndUpdatedTS = tacitAutoIndUpdatedTS;
    }

    public String getTacitNoOfDays() {
        return tacitNoOfDays;
    }

    public void setTacitNoOfDays(String tacitNoOfDays) {
        this.tacitNoOfDays = tacitNoOfDays;
    }

    public String getNonTacitNoOfDays() {
        return nonTacitNoOfDays;
    }

    public void setNonTacitNoOfDays(String nonTacitNoOfDays) {
        this.nonTacitNoOfDays = nonTacitNoOfDays;
    }

    public String getTacitTaskNoOfDays() {
        return tacitTaskNoOfDays;
    }

    public void setTacitTaskNoOfDays(String tacitTaskNoOfDays) {
        this.tacitTaskNoOfDays = tacitTaskNoOfDays;
    }

    public String getMainBusinessArea() {
        return mainBusinessArea;
    }

    public void setMainBusinessArea(String mainBusinessArea) {
        this.mainBusinessArea = mainBusinessArea;
    }

    public String getDefaultBusAreaInd() {
        return defaultBusAreaInd;
    }

    public void setDefaultBusAreaInd(String defaultBusAreaInd) {
        this.defaultBusAreaInd = defaultBusAreaInd;
    }

    public String getModifyExtBusInd() {
        return modifyExtBusInd;
    }

    public void setModifyExtBusInd(String modifyExtBusInd) {
        this.modifyExtBusInd = modifyExtBusInd;
    }

    public Date getDefaultBusAreaUpdTs() {
        return defaultBusAreaUpdTs;
    }

    public void setDefaultBusAreaUpdTs(Date defaultBusAreaUpdTs) {
        this.defaultBusAreaUpdTs = defaultBusAreaUpdTs;
    }

    public Date getModifyExtBusUpdTs() {
        return modifyExtBusUpdTs;
    }

    public void setModifyExtBusUpdTs(Date modifyExtBusUpdTs) {
        this.modifyExtBusUpdTs = modifyExtBusUpdTs;
    }

    public String getTacitAutoNoOfDays() {
        return tacitAutoNoOfDays;
    }

    public void setTacitAutoNoOfDays(String tacitAutoNoOfDays) {
        this.tacitAutoNoOfDays = tacitAutoNoOfDays;
    }

    public String getTacitNotificationInd() {
        return tacitNotificationInd;
    }

    public void setTacitNotificationInd(String tacitNotificationInd) {
        this.tacitNotificationInd = tacitNotificationInd;
    }

    public String getNonTacitNotificationInd() {
        return nonTacitNotificationInd;
    }

    public void setNonTacitNotificationInd(String nonTacitNotificationInd) {
        this.nonTacitNotificationInd = nonTacitNotificationInd;
    }

    public Date getTacitNotiIndUpdatedTS() {
        return tacitNotiIndUpdatedTS;
    }

    public void setTacitNotiIndUpdatedTS(Date tacitNotiIndUpdatedTS) {
        this.tacitNotiIndUpdatedTS = tacitNotiIndUpdatedTS;
    }

    public Date getNonTacitNotiIndUpdatedTS() {
        Date returnDate = null;
        if (nonTacitNotiIndUpdatedTS != null) {
            returnDate = new Date(nonTacitNotiIndUpdatedTS.getTime());
        }
        return returnDate;
    }

    public void setNonTacitNotiIndUpdatedTS(Date nonTacitNotiIndUpdatedTS) {
        if (nonTacitNotiIndUpdatedTS != null) {
            this.nonTacitNotiIndUpdatedTS = new Date(nonTacitNotiIndUpdatedTS.getTime());
        }
    }

    public String getTacitTaskNotificationInd() {
        return tacitTaskNotificationInd;
    }

    public void setTacitTaskNotificationInd(String tacitTaskNotificationInd) {
        this.tacitTaskNotificationInd = tacitTaskNotificationInd;
    }

    public Date getTacitTaskNotiIndUpdatedTS() {
        return tacitTaskNotiIndUpdatedTS;
    }

    public void setTacitTaskNotiIndUpdatedTS(Date tacitTaskNotiIndUpdatedTS) {
        this.tacitTaskNotiIndUpdatedTS = tacitTaskNotiIndUpdatedTS;
    }

    public String getDefaultDetachTab() {
        return defaultDetachTab;
    }

    public void setDefaultDetachTab(String defaultDetachTab) {
        this.defaultDetachTab = defaultDetachTab;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
}
