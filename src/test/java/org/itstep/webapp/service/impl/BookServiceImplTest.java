package org.itstep.webapp.service.impl;

import org.itstep.webapp.repository.BookRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookServiceImplTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    BookRepo repo;

    @BeforeEach
    void setUp() {


    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void countBooks() {
    }

    @Test
    void searchBook() {
    }
}