package com.blog_platform.rough;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class Controller {
    private final PersonRepository personRepository;

    @Autowired
    public Controller(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }
    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "name", "Rough",
                "version", "1.0.0",
                "description", "A simple blog platform"
        );
    }
//    public String home() {
//        return "Hello, World!";
//    }
    @PostMapping("/person")
    public Person createPerson(@RequestBody PersonRequest personRequest) {
        Person person = new Person(personRequest.getName(),personRequest.getAge());
        return personRepository.save(person);
    }
}


class PersonRequest {
    private String name;
    private int age;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}