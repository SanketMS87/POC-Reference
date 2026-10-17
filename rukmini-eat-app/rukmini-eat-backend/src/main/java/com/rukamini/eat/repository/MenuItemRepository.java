package com.rukamini.eat.repository;

import com.rukamini.eat.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
