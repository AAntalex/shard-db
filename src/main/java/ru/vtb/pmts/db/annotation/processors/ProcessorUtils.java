package ru.vtb.pmts.db.annotation.processors;

import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProcessorUtils {
    public static final String CLASS_REPOSITORY_POSTFIX = "$RepositoryImpl";
    public static final String CLASS_MAPPER_POSTFIX = "$MapperImpl";
    public static final String CLASS_INTERCEPT_POSTFIX = "$Interceptor";

    public static Map<String, String> getMethodsByPrefix(Element classElement, String prefix) {
        return ElementFilter.methodsIn(classElement.getEnclosedElements())
                .stream()
                .map(e -> e.getSimpleName().toString())
                .filter(it -> it.startsWith(prefix))
                .collect(Collectors.toMap(String::toLowerCase, it -> it));
    }

    public static String findGetter(Map<String, String> getters, Element element, boolean isFluent) {
        return isFluent ?
                element.getSimpleName().toString() :
                getters.get("get" + element.getSimpleName().toString().toLowerCase());
    }

    public static String findSetter(Map<String, String> setters, Element element, boolean isFluent) {
        return isFluent ?
                element.getSimpleName().toString() :
                setters.get("set" + element.getSimpleName().toString().toLowerCase());
    }

    public static String getPackage(String className) {
        return Optional.of(className.lastIndexOf('.'))
                .filter(it -> it > 0)
                .map(it -> className.substring(0, it))
                .orElse(null);
    }

    public static <A extends Annotation> boolean isAnnotationPresent(Element element, Class<A> annotation) {
        return Optional.ofNullable(element.getAnnotation(annotation))
                .isPresent();
    }

    public static <A extends Annotation> boolean isAnnotationPresentByType(Element element, Class<A> annotation) {
        return Objects.nonNull(
                getDeclaredType(element)
                        .asElement()
                        .getAnnotation(annotation)
        );
    }

    public static String getTypeField(Element element) {
        DeclaredType type = getDeclaredType(element);
        return IntStream.range(0, type.getTypeArguments().size())
                .mapToObj(idx ->
                        (idx == 0 ? "<" : ",") +
                                ((DeclaredType) type.getTypeArguments().get(idx)).asElement().getSimpleName() +
                                (idx == type.getTypeArguments().size() - 1 ? ">" : StringUtils.EMPTY)
                )
                .reduce(type.asElement().getSimpleName().toString(), String::concat);
    }

    public static String getFinalType(Element element) {
        DeclaredType type = getDeclaredType(element);
        return type.getTypeArguments().size() > 0 ?
                ((DeclaredType) type.getTypeArguments().get(0)).asElement().getSimpleName().toString() :
                type.asElement().getSimpleName().toString();
    }

    public static DeclaredType getDeclaredType(Element element) {
        return (DeclaredType) Optional.ofNullable(element)
                .map(Element::asType)
                .orElse(null);
    }

    public static <A extends Annotation> boolean isAnnotationPresentInArgument(Element element, Class<A> annotation) {
        return Optional.ofNullable(getDeclaredType(element))
                .filter(it ->
                        it.getTypeArguments().size() > 0 &&
                                Objects.nonNull(
                                        ((DeclaredType) it.getTypeArguments().get(0))
                                                .asElement()
                                                .getAnnotation(annotation)
                                )
                )
                .isPresent();
    }

    public static Class<?> getClassByType(TypeMirror type) {
        return getClassByName(type.toString());
    }

    public static Class<?> getClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException err) {
            return null;
        }
    }

    public static String getImportedTypes(List<String> importedTypes) {
        return importedTypes.stream()
                .filter(it -> !it.startsWith("java.lang."))
                .distinct()
                .sorted()
                .map(it -> "import " + it + ";\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

}
