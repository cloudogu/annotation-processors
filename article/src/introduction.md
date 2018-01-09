# Java Annotation Prozessoren

Java Annotation Prozessoren sind ein sehr mächtiges Werkzeug im Werkzeugkasten eines Entwicklers. Mit ihnen lassen sich z.B. Informationen zur Build-Zeit protokollieren, der Build kann mit einer Fehlermeldung abgebrochen werden, es können Konfigurationen und Dokumentationen erzeugt, Klassen verändert oder neu erstellt werden.

In dem ersten Abschnitt des Artikels betrachten wir die grundsätzliche Funktionsweise von Annotation Prozessoren anhand eines kleinen Beispiels. In den beiden weiteren Abschnitten werden wir uns das Erzeugen von Konfigurationen und das Generieren von Code genauer ansehen. Der in diesem Artikel verwendete Code, kann unter [https://github.com/cloudogu/annotation-processors/](https://github.com/cloudogu/annotation-processors) eingesehen werden.

Annotation Prozessoren werden während des Builds vom Compiler (javac) aufgerufen, wenn eine der konfigurierten Annotationen gefunden wurde. Dabei kann ein Annotation Prozessor bestimmen, über welche Annotationen er benachrichtigt werden will. Das können eine einzelne, mehrere oder alle Annotationen sein. Wenn der Compiler eine Annotation findet, wird überprüft, ob für diese ein Prozessor registriert wurde. Ist dies der Fall, wird er ausgeführt. An diesem Punkt kann der Annotation Prozessor seine Arbeit verrichten und entscheiden, ob für die gefundene Annotation noch weitere Prozessoren aufgerufen werden dürfen.

## OpenSource Beispiele

Ein prominentes Beispiel für einen Annotation Prozessor ist der `"META-INF/services generator"` von Kohsuke Kawaguchi der aus einer `MetaInfServices` Annotation die Konfiguration für den `Java 6 ServiceLoader` erzeugen kann.

Ein weiteres Beispiel ist das `"Project Lombok"`. Es verspricht, mit einer Reihe von Annotationen, den Boilerplate Code von Java Klassen automatisch zu generieren, z.B. Getter, Setter, hashCode- oder equals-Methoden.

## Ein einfacher Annotation Prozessor

Annotation Prozessoren müssen das Interface `javax.annotation.processing.Processor` implementieren, in den meisten Fällen empfiehlt es sich die Klasse `javax.annotation.processing.AbstractProcessor` zu erweitern, da sie nützliche Hilfsmethoden enthält. Unser Beispiel Annotation Prozessor soll eine Meldung ausgeben, wenn eine bestimmte Annotation gefunden wurde. Dafür erstellen wir zuerst unsere Annotation
(`com/cloudogu/blog/annotationprocessor/log/Log.java`):

```java
@Target({ElementType.TYPE})
public @interface Log {}
```

Die `Target` Annotation mit dem Parameter `ElementType.TYPE` an unserer `Log` Annotation bestimmt, dass wir `@Log` an allen Java Typen (Klassen, Interfaces oder Enums) verwenden können.
Immer wenn Javac diese Annotation findet, wollen wir eine Meldung auf der Konsole ausgeben, die uns zeigt, welche Klasse die Annotation verwendet.
Der Annotation Prozessor
(`com/cloudogu/blog/annotationprocessor/log/LogProcessor.java`) zu der `Log` Annotation:

```java
@SupportedAnnotationTypes("com.cloudogu.blog.annotationprocessor.log.Log") 
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LogProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for ( TypeElement annotation : annotations ) {
      for ( Element element : roundEnv.getElementsAnnotatedWith(annotation) ) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @Log at " + element);
      }
    }
    return true;
  }

}
```

Die `SupportedAnnotationTypes` Annotation bestimmt, für welche Annotationen unser Prozessor aufgerufen wird. Es ist auch möglich „*“ anzugeben, in diesem Fall wird der Prozessor für jede gefundene Annotation aufgerufen. Die `SupportedSourceVersion` gibt an welches die letzte Java Version ist, mit der der Annotation Prozessor umgehen kann. Wenn der Annotation Prozessor mit einer neueren Java Version verwendet wird, wird eine Warnung ausgegeben, dass der Prozessor diese Java Version nicht unterstützt.

Schließlich müssen wir die process Methode des `AbstractProcessor` implementieren. Der Methode werden zwei Werte übergeben:

* Ein Set von `java.lang.model.element.TypeElement`, dieses Set enthält alle gefundenen Annotationen
* `javax.annotation.processing.RoundEnvironment` - mit diesem Objekt kann man die gefundenen, annotierten Elemente untersuchen

Wenn die `process` Methode `true` zurückgibt, werden keine weiteren Annotation Prozessoren für die gefundene Annotation aufgerufen. Wenn sie `false` zurückgibt, können weitere Annotation Prozessoren für diese Annotation benachrichtigt werden. Erweitert man den `AbstractProcessor`, kann man außerdem auf die Variable `processingEnv` vom Typ `javax.annotation.processing.ProcessingEnvironment` zugreifen.  `ProcessingEnvironment` erlaubt es, auf die Umgebung des Compilers zuzugreifen, um zum Beispiel den Build-Prozess abzubrechen oder eine Meldung auf der Konsole auszugeben.

Für unser Beispiel:

1. Iterieren wir erst über das Set der gefundenen Annotationen:

	```java
	for ( TypeElement annotation : annotations ) {
	```

2. Suchen wir dann mit Hilfe des `RoundEnvironment` für jede Annotation, die Elemente die mit dieser Annotation annotiert wurden:

	```java
	for ( Element element : roundEnv.getElementsAnnotatedWith(annotation) ) {
	```

3. Und geben anschließend alle gefundenen Elemente, mit dem `ProcessingEnvironment`, als Information auf der Konsole aus:

	```java
	processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @Log at " + element);
	```

### Registration

Damit der Compiler den Annotation Prozessor finden kann, muss dieser registriert werden. Dies erfolgt über den mit Java 6 eingeführten `ServiceLoader`. Dafür müssen wir eine Datei unter `META-INF/services/javax.annotation.processing.Processor` anlegen, die den vollständigen Namen unseres Annotation Prozessors enthält. In unserem Beispiel wäre das:
`com.cloudogu.blog.annotationprocessor.log.LogProcessor`

### Verwendung

Um den Prozessor verwenden zu können, brauchen wir eine Klasse, die mit `@Log` annotiert ist, zum Beispiel
(`com/cloudogu/blog/annotationprocessor/sample/Hello.java`):

```java
@Log
public class Hello {

  public static void main(String[] args) {
    System.out.println("Hello");
  }

}
```

Nach dem Anlegen der Beispiel-Klasse können wir kompilieren und unseren Annotation Prozessor testen. Zuerst kompilieren wir die Annotation und den Prozessor:

```bash
javac –cp . -proc:none com/cloudogu/blog/annotationprocessor/log/*.java
```

Der Parameter `-proc:none` deaktiviert alle Annotation Prozessoren. Das ist wichtig, da der Compiler unsere Registrierung (die Datei unter `META-INF/services`) bereits findet, aber unser Prozessor noch nicht kompiliert ist. Das würde zu einem Fehler führen.
Jetzt können wir unsere Test-Klasse kompilieren und damit unseren Annotation Prozessor testen:

```bash
javac –cp . com/cloudogu/blog/annotationprocessor/sample/*.java
```

Jetzt sollte der Compiler den Annotation Prozessor aufrufen und wir sollten folgende Zeile auf der Konsole sehen:

```text
Note: found @Log at com.cloudogu.blog.annotationprocessor.sample.Hello
```

Annotation Prozessoren können auch mit den gängigen Buildtools und IDEs verwendet werden. Um Annotation Prozessoren zum Beispiel mit Maven zu verwenden, muss der Build in separate Module aufgeteilt werden. Die Separierung muss erfolgen, damit Maven den Annotation Prozessor unabhängig und vor Verwendung kompilieren kann. Unser Beispiel mit Maven kann man unter der URL [https://github.com/cloudogu/annotation-processors/tree/master/part-1-maven](https://github.com/cloudogu/annotation-processors/tree/master/part-1-maven) einsehen. Hierbei ist zu beachten, dass das `maven-compiler-plugin` im Modul `sample` so konfiguriert ist, dass Compiler-Warnings ausgegeben werden (`showWarnings` auf `true`). Führt man nun `mvn clean install` im `parent`-Modul aus, erscheint die erwartete Meldung im Output:

```text
[INFO] found @Log at com.cloudogu.blog.annotationprocessor.sample.Hello
```
