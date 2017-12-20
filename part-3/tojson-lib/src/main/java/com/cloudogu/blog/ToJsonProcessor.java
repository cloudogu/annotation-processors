/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Cloudogu GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.cloudogu.blog;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.cloudogu.blog.JsonObject")
public class ToJsonProcessor extends AbstractProcessor {

    private static final String TEMPLATE = "com/cloudogu/blog/jsonwriter.mustache";

    private final Mustache template;

    public ToJsonProcessor() {
        this.template = createTemplate();
    }

    private Mustache createTemplate() {
        MustacheFactory factory = new DefaultMustacheFactory();
        return factory.compile(TEMPLATE);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for ( TypeElement annotation : annotations ) {
            for ( Element element : roundEnv.getElementsAnnotatedWith(annotation) ) {
                try {
                    processJsonObject(element);
                } catch (IOException e) {
                    error(e);
                }
            }
        }
        return true;
    }

    private void processJsonObject(Element element) throws IOException {
        if (isTypeElement(element)) {
            TypeElement typeElement = (TypeElement) element;
            Scope scope = createModel(typeElement);
            writeJsonWriterClass(element, scope);
        }
    }

    private void writeJsonWriterClass(Element element, Scope scope) throws IOException {
        Filer filer = processingEnv.getFiler();
        JavaFileObject fileObject = filer.createSourceFile(scope.getTargetClassNameWithPackage(), element);
        try (Writer writer = fileObject.openWriter()) {
            template.execute(writer, scope);
        }
    }

    private Scope createModel(TypeElement element) {
        String packageName = getPackageName(element);
        String sourceClassName = getSimpleNameAsString(element);

        Scope scope = new Scope(packageName, sourceClassName);
        appendFields(element, scope);
        return scope;
    }

    private String getSimpleNameAsString(Element element) {
        return element.getSimpleName().toString();
    }

    private String getPackageName(TypeElement classElement) {
        return ((PackageElement) classElement.getEnclosingElement()).getQualifiedName().toString();
    }

    private void appendFields(TypeElement element, Scope scope) {
        if (isTypeElement(element)) {
            for (String getterMethod : getAllGetterMethodNames(element)) {
                scope.addGetter(getterMethod);
            }
        }
    }

    private boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }

    private List<String> getAllGetterMethodNames(TypeElement typeElement) {
        return processingEnv.getElementUtils().getAllMembers(typeElement)
                .stream()
                .filter(el -> el.getKind() == ElementKind.METHOD)
                .map(el -> getSimpleNameAsString(el))
                .filter(name -> name.startsWith("get"))
                .collect(Collectors.toList());
    }

    private void error(IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "failed to write extension file: " + e.getMessage());
    }
}
