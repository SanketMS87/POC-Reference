package com.rukamini.mail.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "T_TEMPLATE")
@NoArgsConstructor
public class TTemplate {

    @Id
    @Column(name = "TEMPLATE_ID")
    private String templateId;

    @Column(name = "TEMPLATE_NAME")
    private String templateName;

    @Column(name = "TEMPLATE_SUBJECT")
    private String templateSubject;

    @Column(name = "TEMPLATE_BODY")
    private String templateBody;

    @Column(name = "ACTIVE_IND")
    private String activeInd;

    @Column(name = "TO_LIST")
    private String toList;

    @Column(name = "CC_LIST")
    private String ccList;
}
