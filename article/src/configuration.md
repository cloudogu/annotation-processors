## Konfigurationsdateien generieren

Im zweiten Abschnitt wollen wir uns dem Erzeugen von Konfigurationsdateien für eine einfache Plugin Bibliothek widmen. Dafür werden wir einen Annotation Prozessor schreiben, der alle Klassen, die mit einer `@Extension` Annotation versehen wurden, in eine XML-Datei schreibt. Zudem vollständigen Namen der Klasse soll außerdem noch das Javadoc der Klasse mit in die XML-Datei geschriben werden. Zusätzlich werden wir eine Klasse schreiben die es uns erlaubt diese Dateien aus dem Classpath auszulesen. 

Es ist auch möglich alle Klassen mit einer `@Extension` Annotation zu finden, ohne einen Annotation Prozessor zu verwenden. Dafür müsste man aber alle Element des Classpath (Ordner und Jar-Dateien) öffnen, jede Klasse laden und mit `Reflection` überprüfen, ob die Klasse die gesuchte Annotation hat. Dieses Vorgehen ist sehr viel aufwändiger, anfälliger für Fehler und deutlich langsamer.

### Die Extension Annotation

```java
@Documented
@Target(ElementType.TYPE)
public @interface Extension {
}
```

Die Extension Annotation ähnelt sehr der Log-Annotation aus dem ersten Abschnitt, mit Ausnahme der `Documented` Annotation. `@Documented` sorgt dafür, dass unsere Annotation im Javadoc der annotierten Klasse auftaucht.

### Der Extension Annotation Prozessor

Der `ExtensionProcessor` sammelt zuerst alle Klassen die mit unserer Extension Annotation versehen wurden in einem Set:

```java
Set<ExtensionDescriptor> descriptors = new LinkedHashSet<>();
for ( TypeElement annotation : annotations ) {
    for ( Element extension : roundEnv.getElementsAnnotatedWith(annotation) ) {
        ExtensionDescriptor descriptor = createDescriptor(extension);
        descriptors.add(descriptor);
    }
}
```

Die `createDescriptor` Methode speichert dabei den Namen und das Javadoc der annotierten Klasse in einer eigenen Klasse namens `ExtensionDescriptor`. Den Namen kann man über den Typ des Elementes erfragen:

```java
extension.asType().toString()
```

Das JavaDoc der Klasse kann man über `Elements` des `ProcessingEnvironment`s erfragen:

```java
processingEnv.getElementUtils().getDocComment(extension).trim()
```

Nachdem wir alle Extensions gesammelt haben, können wir unsere XML-Datei schreiben. Damit unsere XML-Datei im Classpath verfügbar ist, muss sie in das richtige Verzeichnis geschrieben werden. Das Verzeichnis lässt sich über die `Filer` Klasse des `ProcessingEnvironment`’s herausfinden:

```java
Filer filer = processingEnv.getFiler();
FileObject fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "extensions.xml");
File extensionsFile = new File(fileObject.toUri());
```

Jetzt müssen wir die Extensions Datei nur noch mit Inhalt füllen. Dafür erstellen wir eine `Wrapper` Klasse für unsere `ExtensionDescriptor` Klasse und annotieren beide mit [JAXB](https://docs.oracle.com/javase/tutorial/jaxb/intro/index.html) Annotationen. Anschließend können wir die Extensions-Datei schreiben mit Hilfe von JAXB schreiben:

```java
JAXB.marshal(new ExtensionDescriptorWrapper(descriptors), file);
```

Mit dem `ExtensionProcessor` haben wir jetzt alles um während des Kompilierens alle Klassen, die mit einer `Extension` Annotation versehen wurden, in einer Datei zu speichern. Das Ergebnis sollte ungefähr so aussehen:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<spl-extensions>
    <extensions>
        <className>com.cloudogu.blog.AhoiService</className>
        <description>Says ahoi to someone.</description>
    </extensions>
    <extensions>
        <className>com.cloudogu.blog.HelloService</className>
        <description>Says hello to someone.</description>
    </extensions>
</spl-extensions>
```

Diese Datei sollte sich im selben Verzeichnis wie die kompilierten Klassen befinden (bei Maven `target/classes`).

### Extensions Util

Um die Extensions zur Laufzeit wieder auszulesen, können wir uns leicht eine Hilfsklasse schreiben:

```java
public static List<ExtensionDescriptor> getExtensions() throws IOException {
    List<ExtensionDescriptor> descriptors = new ArrayList<>();
    Enumeration<URL> extensionFiles = Thread.currentThread().getContextClassLoader().getResources(LOCATION);
    while (extensionFiles.hasMoreElements()) {
        URL extensionFile = extensionFiles.nextElement();
        ExtensionDescriptorWrapper extensionDescriptorWrapper = JAXB.unmarshal(extensionFile, ExtensionDescriptorWrapper.class);
        descriptors.addAll(extensionDescriptorWrapper.getExtensions());
    }
    return descriptors;
}
```

Mit dieser Methode werden alle Extension XML-Dateien im Classpath gefunden. Außerdem werden in einer Liste alle Klassen gespeichert, die mit einer Extension Annotation annotiert wurden. Da wir den `ContextClassLoader` des Threads verwenden, können sich unsere Extensions XML-Dateien sogar in verschiedenen JAR-Dateien befinden.

Wenn wir jetzt alle Extension Klassen unserer Anwendung ausgeben wollen, können wir folgenden Code verwenden:

```java
for (ExtensionDescriptor descriptor : Extensions.getExtensions()) {
    System.out.println(descriptor);
}
```

Das gesamte Beispiel kann unter [part-2](https://github.com/cloudogu/annotation-processors/tree/master/part-2) des GitHub Repositories gefunden werden.

### Beispiele aus der Open Source Welt

Ein prominentes Beispiel für einen Annotation Prozessor der Konfigurationsdateien generiert, ist der [META-INF/services generator](http://metainf-services.kohsuke.org/) von Kohsuke Kawaguchi der aus einer `MetaInfServices` Annotation die Konfiguration für den `Java 6 ServiceLoader` erzeugen kann.

Ein weiteres Beispiel ist das Plugin Framework von [SCM-Manager 2.0.0](https://www.scm-manager.org/release/scm-manager-2-milestone-1/). SCM-Manager hat in Version 1 noch Classpath Scanning verwendet um die Erweiterungen zu finden. Durch den Umstieg auf Annotation Prozessoren konnte die Startzeit von SCM-Manager 2 drastisch verkürzt werden.
