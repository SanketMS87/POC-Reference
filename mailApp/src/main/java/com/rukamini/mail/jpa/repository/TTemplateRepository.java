package com.rukamini.mail.jpa.repository;

import com.rukamini.mail.jpa.entity.TTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TTemplateRepository extends JpaRepository<TTemplate, Long> {

    @Query("SELECT t FROM TTemplate t WHERE t.templateName like :templateName")
    TTemplate getMailTemplate(@Param("templateName") String templateName);
}
