# annotation-processors: Part 2

This part demonstrates the dynamic creation of configuration files, using a simple plugin framework.

The example requires Java 8 and Maven >= 3.

Compile the Project:

```
mvn clean install
```

The annotation processor will generate the _extensions.xml_ in _spl-example/target/classes_. The generation is tested by
 [AppTest.java](spl-example/src/test/java/com/cloudogu/blog/AppTest.java) which collects the generated configuration 
 file and checks if it is valid.