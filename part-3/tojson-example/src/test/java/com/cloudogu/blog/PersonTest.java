package com.cloudogu.blog;

import org.junit.Test;

import static org.junit.Assert.*;

public class PersonTest {

    @Test
    public void testJsonWriter() {
        Person person = new Person("tricia", "tricia.mcmillian@hitchhicker.com");
        String json = PersonJsonWriter.toJson(person);
        assertEquals("{\"class\": \"class com.cloudogu.blog.Person\",\"username\": \"tricia\",\"email\": \"tricia.mcmillian@hitchhicker.com\"}", json);
    }

}