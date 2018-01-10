# annotation-processors: Part 3

This part demonstrates the generation of Java source code.

The example requires Java 8 and Maven >= 3.

Compile the Project:

```
mvn clean install
```

The annotation processor will generate the _com.cloudogu.blog.PersonJsonWriter_ class in
 _tojson-example/target/classes_. The generation is tested by
 [PersonTest.java](tojson-example/src/test/java/com/cloudogu/blog/PersonTest.java) which uses the generated class to
 create json for a person object.