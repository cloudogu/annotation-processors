## Source Code generieren

Im dritten und letzten Abschnitt des Artikels wird demonstriert wie man Source Code mit Hilfe eines Annotation Prozessors generieren kann. In unserem Beispiel wollen für jede mit einer `@JsonObject` Annotation versehenen Klasse eine zusätzliche `JsonWriter` Klasse generieren. Die generierten `JsonWriter` Klassen, sollen Json für alle `Getter`-Methoden der Annotierten Klasse erzeugen. Damit ist es möglich annotierte Klassen, in das Json Format zu serialisieren. Konkret soll zu der Klasse `Person`:

```java
@JsonObject
public class Person {

  private String username;
  private String email;

  public Person(String username, String email) {
    this.username = username;
    this.email = email;
  }

  // getter
}
```

automatisch ein `PersonJsonWriter` erzeugt werden, der folgendermaßen aussieht:

```java
public final class PersonJsonWriter {

  public static String toJson(Person object) {
    StringBuilder builder = new StringBuilder("{");
    
    builder.append("\"class\": \"");
    builder.append(object.getClass())
    builder.append("\",");
    
    builder.append("\"username\": \"");
    builder.append(object.getUsername());
    builder.append("\",");
    
    builder.append("\"email\": \"");
    builder.append(object.getEmail());
    builder.append("\"");
    
    return builder.append("}").toString();
  }

}
```

Es wäre auch möglich an die `Person` Klasse eine `toJson` Methode anzufügen, aber das würde die Länge des Artikels sprengen, da wir hierfür die ursprüngliche Klasse parsen müssten.

### Annotierte Klassen finden

Als erstes müssen wir alle Klassen finden, die mit der `JsonObject` Annotation versehen wurden. Das unterscheidet sich in diesem Fall nicht von den beiden ersten Abschnitten, darum sparen wir uns diesmal das Code-Listing. Anschließend müssen wir für jede gefundene Klasse ein `Scope`-Objekt erzeugen, mit dem wir später eine Template-Engine füttern werden. 

```java
public final class Scope {

  private String packageName;
  private String sourceClassName;
  private List<Field> fields = new ArrayList<>();

  Scope(String packageName, String sourceClassName) {
    this.packageName = packageName;
    this.sourceClassName = sourceClassName;
  }

  void addGetter(String getter) {
    String fieldName = getter.substring(3);
    char firstChar = fieldName.charAt(0);
    fieldName = Character.toLowerCase(firstChar) + fieldName.substring(1);
    fields.add(new Field(fieldName, getter));
  }
    
  // getter

  public static class Field {

    private String name;
    private String getter;

    private Field(String name, String getter) {
      this.name = name;
      this.getter = getter;
    }
        
    // getter
  }

}
```

Für das `Scope`-Objekt brauchen wir den Namen der annotierten Klasse und dessen Package. Um an den Namen des Packages zu kommen, müssen wir zunächst sicherstellen, dass es sich bei unserem annotierten Element um ein `TypeElement` handelt:

```java
if (element instanceof TypeElement) {
}
```

Wenn das der Fall ist, können wir das `TypeElement` nach dessen übergeordneten Element fragen und dieses wiederum nach seinem Namen fragen:

```java
private String getPackageName(TypeElement classElement) {
  return ((PackageElement) classElement.getEnclosingElement()).getQualifiedName().toString();
}
```

Jetzt brauchen wir nur noch die Namen aller Getter-Methoden für unser `Scope`-Objekt. Dafür können wir `ElementsUtil`s des `ProcessingEnvironments` verwenden:

```java
processingEnv.getElementUtils().getAllMembers(typeElement)
```

Die `getAllMembers`-Methode gibt uns eine Liste aller Member Elemente unserer Klasse zurück. Aus dieser Liste müssen wir nur noch alle Elemente vom Typ `METHOD`, deren Name mit einem “get” anfängt, herausfiltern. Dafür lässt sich sehr gut die Stream API der Java Collections verwenden, die mit Java 8 eingeführt wurden:

```java
processingEnv.getElementUtils().getAllMembers(typeElement)
  .stream()
  .filter(el -> el.getKind() == ElementKind.METHOD)
  .map(el -> el.getSimpleName().toString())
  .filter(name -> name.startsWith("get"))
  .collect(Collectors.toList()); 
```

Das Listing Zeile für Zeile erklärt:

* Findet alle Member Elemente
* Wandelt die Liste in einen Stream
* Entfernt alle Elemente die nicht vom Typ Method sind
* Extrahiert den Namen des Elements
* Entfernt alle Namen die nicht mit “get” beginnen
* Erstellt aus dem Stream wieder eine Liste

Jetzt haben wir alle Informationen zusammen die wir brauchen um den `JsonWriter` zu erstellen.

### JsonWriter schreiben

Um den `JsonWriter` zuschreiben, kann abermals der `Filer` aus dem `ProcessingEnvironment` verwendet werden:

```java
Filer filer = processingEnv.getFiler();
JavaFileObject fileObject = filer.createSourceFile(scope.getTargetClassNameWithPackage(), element);
```

Der `createSourceFile` Methode muss man den gewünschten Klassennamen und das annotierte Element übergeben, um ein `JavaFileObject` zu erhalten. Mit diesem `JavaFileObject` kann man anschließend einen `Writer` öffnen:

```java
Writer writer = fileObject.openWriter();
```

Dieser `Writer` schreibt dann eine Java-Datei in den Ordner des Packages in den Klassenpfad (mit [Maven](https://maven.apache.org) werden von Annotation Prozessoren erstellte Klassen unter `target/generated-sources/annotations` abgelegt).

Wir könnten nun den Source Code direkt mit dem `Writer` schreiben, aber man verliert schnell den Überblick durch das Escaping der Hochkommas. 

Eine andere Möglichkeit den Quellcode aus dem `Scope`-Objekt zu erzeugen, ist [JavaPoet](https://github.com/square/javapoet). JavaPoet bietet eine Java Builder-API um Java-Dateien zu erzeugen. Die Verwendung von JavaPoet, würde aber den Rahmen des Artikels sprengen, deshalb begnügen wir uns mit einer einfachen Template-Engine für unser Beispiel.

Wir werden die [Java Implementation](https://github.com/spullara/mustache.java) der Template-Engine [Mustache](https://mustache.github.io/) verwenden. Mustache Templates sind sehr einfach aufgebaut und die Syntax ist schnell erlernt. 

Um unser Beispiel zu verstehen, reicht es zu wissen, dass
* mit dem Ausdruck `{{sourceClassName}}` auf die Getter-Methode `getSourceClassName` des `Scope`-Objektes zugegriffen wird
* mittels `{{#fields}}...{{/fields}}` über die Collection der Fields Variable des `Scope`-Objektes iteriert wird und
* `{{^last}}...{{/last}}` prüft, dass das Feld nicht das letzte Element in der Collection ist.

```java
package {{packageName}};

public final class {{targetClassName}} {

  public static String toJson({{sourceClassName}} object) {
    StringBuilder builder = new StringBuilder("{");
    
    {{#fields}}
    builder.append("\"{{value.name}}\": \"");
    builder.append(object.{{value.getter}}());
    builder.append("\"{{^last}},{{/last}}");
    {{/fields}}
    
    return builder.append("}").toString();
  }

}
```

Mit folgendem Code wird das Mustache Template aus dem Classpath gelesen, mit dem Scope-Objekt ausgeführt und in den Writer des JavaFileObjectes geschrieben:

```java
MustacheFactory factory = new DefaultMustacheFactory();
Template template = factory.compile("com/cloudogu/blog/jsonwriter.mustache");
template.execute(writer, scope);
```

Jetzt haben wir alles zusammen um den `PersonJsonWriter` zu generieren. Dafür kompilieren wir die, mit der `@Json` annotierten, `Person` Klasse mit unserem Annotation Prozessor im Classpath. Anschließend sollten wir die `PersonJsonWriter` Klasse im `target/classes` Verzeichnis finden. Verwenden können wir die Klasse wie folgt:

```java
Person person = new Person("tricia", "tricia.mcmillian@hitchhicker.com");
String json = PersonJsonWriter.toJson(person);
System.out.println(json);
```

Das obere Listing sollte den folgenden Json-String ausgeben:

```json
{
  "class": "class com.cloudogu.blog.Person",
  "username": "tricia",
  "email": "tricia.mcmillian@hitchhicker.com"
}
```

### Open Source Beispiele

Prominente Beispiele für Annotation Prozessoren die Quellcode generieren:

* [Hibernate Metamodel Generator](http://hibernate.org/orm/tooling/) generiert ein Metamodel aus JPA-Entities, um die JPA-Criteria-API typensicher zu verwenden.
* [QueryDSL](http://www.querydsl.com/) bietet ein Konzept, um Abfragen für Java-Entities in SQL-nahen
Sprachen zu formulieren. Dabei werden Annotation Prozessoren verwendet, um die API für die Abfragen aus den Entieties zu generieren.
* [Project Lombok](https://projectlombok.org/) verspricht, mit einer Reihe von Annotationen, den Boilerplate Code von Java Klassen automatisch zu generieren, z.B. Getter, Setter, `hashCode`- oder `equals`-Methoden.
