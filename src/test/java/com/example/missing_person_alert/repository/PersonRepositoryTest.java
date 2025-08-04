package com.example.missing_person_alert.repository;

import com.example.missing_person_alert.entity.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryTest {
    @Autowired
    private PersonRepository personRepository;

    @Test
    void testFindNonExpired() {
        LocalDateTime now = LocalDateTime.now();

        // Person that is already expired
        Person expired = new Person();
        expired.setFullName("Expired Person");
        expired.setImagePath("/uploads/expired.jpg");
        expired.setPublishedAt(now.minusDays(5));
        expired.setExpiresAt(now.minusHours(1));
        expired.setLastSeenLatitude(50.0);
        expired.setLastSeenLongitude(8.0);
        personRepository.save(expired);

        // Person that is not yet expired
        Person valid = new Person();
        valid.setFullName("Active Person");
        valid.setImagePath("/uploads/active.jpg");
        valid.setPublishedAt(now.minusHours(2));
        valid.setExpiresAt(now.plusDays(3));
        valid.setLastSeenLatitude(52.5);
        valid.setLastSeenLongitude(13.4);
        personRepository.save(valid);

        List<Person> nonExpired = personRepository.findNonExpired();

        assertEquals(1, nonExpired.size());
        assertEquals("Active Person", nonExpired.get(0).getFullName());
    }
}