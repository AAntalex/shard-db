package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.*;
import com.antalex.db.model.dto.*;
import com.antalex.db.model.enums.MappingType;
import lombok.experimental.Accessors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class CriteriaClassBuilder {
    private static final Map<Element, CriteriaClassDto> criteriaClasses = new HashMap<>();

    public static CriteriaClassDto getClassDtoByElement(Element classElement) {
        Criteria criteria = classElement.getAnnotation(Criteria.class);
        if (criteria == null) {
            return null;
        }
        if (!criteriaClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();
            EntityClassDto entityClass = EntityClassBuilder.getClassDtoByElement(getElement(criteria));

            boolean isFluent = Optional.ofNullable(classElement.getAnnotation(Accessors.class))
                    .map(Accessors::fluent)
                    .orElse(false);
            Map<String, String> setters = ProcessorUtils.getMethodsByPrefix(classElement, "set");

            CriteriaClassDto criteriaClassDto = CriteriaClassDto
                    .builder()
                    .targetClassName(elementName)
                    .classPackage(ProcessorUtils.getPackage(classElement.asType().toString()))
                    .from(entityClass)
                    .alias(criteria.alias().toUpperCase())
                    .where(criteria.where())
                    .joins(
                            Arrays.stream(criteria.joins())
                                    .map(CriteriaClassBuilder::getJoinDto)
                                    .toList()
                    )
                    .build();

            Map<String, EntityClassDto> entityClasses = criteriaClassDto
                    .getJoins()
                    .stream()
                    .collect(Collectors.toMap(CriteriaJoinDto::getAlias, CriteriaJoinDto::getFrom));
            entityClasses.put(criteriaClassDto.getAlias(), criteriaClassDto.getFrom());
            criteriaClassDto.setFields(
                    ElementFilter.fieldsIn(classElement.getEnclosedElements())
                            .stream()
                            .map(
                                    fieldElement ->
                                            CriteriaFieldDto
                                                    .builder()
                                                    .fieldName(fieldElement.getSimpleName().toString())
                                                    .setter(
                                                            ProcessorUtils.
                                                                    findSetter(setters, fieldElement, isFluent)
                                                    )
                                                    .element(fieldElement)
                                                    .entityField(
                                                            getColumn(fieldElement, entityClass, entityClasses))
                                                    .build()
                            )
                            .toList()
            );

            criteriaClasses.put(classElement, criteriaClassDto);
        }
        return criteriaClasses.get(classElement);
    }

    private static CriteriaJoinDto getJoinDto(Join join) {
        return CriteriaJoinDto
                .builder()
                .from(EntityClassBuilder.getClassDtoByElement(getElement(join)))
                .alias(join.alias().toUpperCase())
                .on(join.on())
                .joinType(join.joinType())
                .build();
    }

    private static String getColumnName(String attribute,
                                        EntityClassDto entityClass,
                                        Map<String, EntityClassDto> entityClasses)
    {
        int idx = attribute.indexOf(".");
        String alias = null;
        if (idx > 0) {
            alias = attribute.substring(0, idx);
            attribute = attribute.substring(idx + 1);
        } else if (entityClasses.containsKey(attribute)) {
            return
        }


        return Optional.ofNullable(element.getAnnotation(Attribute.class))
                .filter(a -> a.mappingType() == MappingType.ENTITY)
                .map(a ->
                        entityClass.getFieldMap().get(
                                a.name().isEmpty() ?
                                        element.getSimpleName().toString() :
                                        a.name()
                        )
                )
                .orElse(null);
    }


    private static String getColumnName(EntityClassDto entityClass, String fieldName) {
        return Optional.ofNullable(entityClass.getFieldMap().get(fieldName))
                .map(EntityFieldDto::getColumnName)
                .orElseGet(fieldName::toUpperCase);
    }

    private static String getColumn(
            Element element,
            EntityClassDto entityClass,
            Map<String, EntityClassDto> entityClasses
    ) {
        return Optional.ofNullable(element.getAnnotation(CriteriaAttribute.class))
                .map(a ->
                        a.value().isEmpty() ?
                                getColumnName(entityClass, element.getSimpleName().toString()) :
                                ""
                )
                .orElse(null);
    }

    private static Element getElement(Criteria criteria) {
        try {
            criteria.from();
            throw new IllegalArgumentException("Is not element");
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
    }

    private static Element getElement(Join join) {
        try {
            join.from();
            throw new IllegalArgumentException("Is not element");
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
    }

    public static void createRepositoryClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        CriteriaClassDto criteriaClassDto = getClassDtoByElement(annotatedElement);
        if (criteriaClassDto == null) {
            return;
        }
        String className = criteriaClassDto.getTargetClassName() + ProcessorUtils.CLASS_REPOSITORY_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + criteriaClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    getImportedTypes(
                            criteriaClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            Optional.class.getCanonicalName(),
                                    )
                            )
                    )
            );
            out.println(
                    "public class " + className + " extends " + criteriaClassDto.getTargetClassName() + " {\n" +
                            "    private DomainEntityManager domainManager;\n" +
                            getLazyFlagsCode(domainClassDto) +
                            "\n    public " + className + "(ShardEntityManager entityManager) {\n" +
                            "        this.entityManager = entityManager;\n" +
                            "    }\n"
            );
            out.println("}");
        }
    }

    private static String getImportedTypes(CriteriaClassDto criteriaClassDto, List<String> importedTypes) {
        criteriaClassDto.getFields()
                .stream()
                .map(CriteriaFieldDto::getElement)
                .filter(element -> ProcessorUtils.isAnnotationPresent(element, CriteriaAttribute.class))
                .map(ProcessorUtils::getDeclaredType)
                .filter(Objects::nonNull)
                .forEach(type -> importedTypes.add(type.asElement().toString()));
        return ProcessorUtils.getImportedTypes(importedTypes);
    }
}