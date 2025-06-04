package com.blog_platform.rough;


import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
    // Custom query methods can be defined here if needed
    // For example, findByName(String name) or findByAge(int age)
}
