package com.example.missing_person_alert.repository;

import com.example.missing_person_alert.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    @Query("SELECT p FROM Person p WHERE p.expiresAt > CURRENT_TIMESTAMP")
    List<Person> findNonExpired();
}