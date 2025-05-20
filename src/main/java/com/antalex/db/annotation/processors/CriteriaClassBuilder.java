package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.*;
import com.antalex.db.model.dto.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CriteriaClassBuilder {
    private static final Map<Element, CriteriaClassDto> CRITERIA_CLASSES = new HashMap<>();
    private static final String PATTERN_JOIN_ON = "^(\\w+\\.)?\\w+=(\\w+\\.)?\\w+$";

    public static CriteriaClassDto getClassDtoByElement(Element classElement) {
        Criteria criteria = classElement.getAnnotation(Criteria.class);
        if (criteria == null) {
            return null;
        }
        if (!CRITERIA_CLASSES.containsKey(classElement)) {
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
                                                    .domainClass(getDomainClass(fieldElement, entityClasses))
                                                    .columnName(
                                                            getColumnName(
                                                                    fieldElement,
                                                                    criteriaClassDto.getAlias(),
                                                                    entityClasses)).
                                                    build()
                            )
                            .toList()
            );
            Set<String> aliases = entityClasses.keySet();
            Map<String, Long> entityColumns = aliases
                    .stream()
                    .collect(Collectors.toMap(item -> item, item -> 0L));
            AtomicInteger idx = new AtomicInteger();
            criteriaClassDto
                    .getFields()
                    .stream()
                    .filter(it -> Objects.nonNull(it.getColumnName()))
                    .forEachOrdered(field -> {
                        field.setColumnIndex(idx.incrementAndGet());
                        if (field.getColumnIndex() > Long.SIZE) {
                            throw new IllegalArgumentException(
                                    "Количество аттрибутов класса с аннотацией @Criteria не может превышать " +
                                            Long.SIZE);
                        }
                        String alias = getAliasFromColumn(field.getColumnName(), aliases)
                                .orElse(criteriaClassDto.getAlias());
                        entityColumns.put(alias, entityColumns.get(alias) | 1L << (field.getColumnIndex() - 1));
                    });
            criteriaClassDto.setColumns(entityColumns.get(criteriaClassDto.getAlias()));
            criteriaClassDto
                    .getJoins()
                    .forEach(join -> join.setColumns(entityColumns.get(join.getAlias())));
            CRITERIA_CLASSES.put(classElement, criteriaClassDto);
        }
        return CRITERIA_CLASSES.get(classElement);
    }

    private static void parseOn(CriteriaJoinDto joinDto, Map<String, EntityClassDto> entityClasses) {
        String on = joinDto.getOn().toUpperCase().replaceAll("(\\r|\\n|\\t|\\s)", "");
        if (!Pattern.compile(PATTERN_JOIN_ON).matcher(on).matches()) {
            throw new IllegalArgumentException(
                    "Условие соединения '" + joinDto.getOn() + "' не соответствует шаблону: " + PATTERN_JOIN_ON);
        }


        int idx = on.indexOf("=");
        EntityAttribute entityAttribute = getEntityAttribute(on.substring(0, idx), joinDto.getAlias(), entityClasses);
        checkEntityAttribute(entityAttribute);
        EntityAttribute entityAttribute2 = getEntityAttribute(
                on.substring(idx + 1), joinDto.getAlias(), entityClasses);
        checkEntityAttribute(entityAttribute2);

    }

    private static EntityAttribute getEntityAttribute(
            String attributeName,
            String defaultAlias,
            Map<String, EntityClassDto> entityClasses)
    {
        int idx = attributeName.indexOf(".");
        EntityAttribute entityAttribute = new EntityAttribute();
        if (idx > 0) {
            entityAttribute.setAlias(attributeName.substring(0, idx));
            entityAttribute.setFieldName(attributeName.substring(idx + 1));
        } else if (entityClasses.containsKey(attributeName)) {
            entityAttribute.setAlias(attributeName);
        } else {
            entityAttribute.setAlias(defaultAlias);
            entityAttribute.setFieldName(attributeName);
        }
        entityAttribute.setEntityClass(entityClasses.get(entityAttribute.getAlias()));
        if (
                Optional
                        .ofNullable(entityAttribute.getFieldName())
                        .map(fieldName -> !"ID".equals(fieldName))
                        .orElse(false)
        ) {
            entityAttribute.setEntityField(
                    Optional.ofNullable(entityAttribute.getEntityClass())
                            .map(EntityClassDto::getFieldMap)
                            .map(fieldMap -> fieldMap.get(entityAttribute.getFieldName()))
                            .orElse(null)
            );
        }
        return entityAttribute;
    }

    private static void checkEntityAttribute(EntityAttribute entityAttribute) {
        if (entityAttribute.getEntityClass() == null) {
            throw new IllegalArgumentException("Не известный синоним " + entityAttribute.getAlias());
        }
        if (
                entityAttribute.getEntityField() == null &&
                        Optional
                                .ofNullable(entityAttribute.getFieldName())
                                .map(fieldName -> !"ID".equals(entityAttribute.getFieldName()))
                                .orElse(false)
        ) {
            throw new IllegalArgumentException(
                    "Отсутствует поле " + entityAttribute.getFieldName() +
                            " в классе " + entityAttribute.getEntityClass().getTargetClassName());
        }
    }

    private static Optional<String> getAliasFromColumn(String columnName, Set<String> aliases) {
        int idx = columnName.lastIndexOf('.');
        if (idx > 0) {
            String alias = columnName.substring(0, idx);
            if (aliases.contains(alias)) {
                return Optional.of(alias);
            }
        }
        return aliases
                .stream()
                .filter(alias -> columnName.contains(alias + "."))
                .max(Comparator.comparing(String::length));
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

    private static DomainClassDto getDomainClass(Element element, Map<String, EntityClassDto> entityClasses) {
        return Optional.ofNullable(element.getAnnotation(CriteriaAttribute.class))
                .map(CriteriaAttribute::value)
                .map(String::toUpperCase)
                .filter(entityClasses::containsKey)
                .map(alias -> {
                    DomainClassDto domainClassDto = DomainClassBuilder.getClassDtoByElement(
                            ProcessorUtils.getDeclaredType(element).asElement()
                    );
                    if (
                            Optional.ofNullable(domainClassDto)
                                    .map(DomainClassDto::getEntityClass)
                                    .filter(it -> it == entityClasses.get(alias))
                                    .isEmpty()
                    ) {
                        throw new RuntimeException(
                                "Класс домена поля " +
                                        element.getSimpleName().toString() +
                                        " не соответствует классу сущности синонима " + alias);
                    }
                    return domainClassDto;
                })
                .orElse(null);
    }

    private static String getColumnName(String mainAlias,
                                        CriteriaAttribute attribute,
                                        Map<String, EntityClassDto> entityClasses)
    {
        String attributeName = attribute.value().toUpperCase();
        int idx = attributeName.indexOf(".");
        String alias = null;
        if (idx > 0) {
            alias = attributeName.substring(0, idx);
            attributeName = attributeName.substring(idx + 1);
        } else if (entityClasses.containsKey(attributeName)) {
            return attributeName + ".ID";
        }
        return Optional
                .ofNullable(getColumnName(alias == null ? mainAlias : alias, attributeName, entityClasses))
                .orElse(attribute.value());
    }

    private static EntityFieldDto getEntityField(
            String alias,
            String fieldName,
            Map<String, EntityClassDto> entityClasses)
    {
        return Optional.ofNullable(entityClasses.get(alias))
                .map(EntityClassDto::getFieldMap)
                .map(fieldMap -> fieldMap.get(fieldName))
                .orElse(null);
    }

    private static String getColumnName(String alias, String fieldName, Map<String, EntityClassDto> entityClasses) {
        EntityFieldDto entityField = getEntityField(alias, fieldName, entityClasses);
        if (entityField == null) {
            return null;
        } else if (entityField.getColumnName() == null) {
            throw new IllegalArgumentException("Для поля " + alias + "." + fieldName +
                    " отсутсвует соответствующая колонка в таблице.");
        } else {
            return alias + "." + entityField.getColumnName();
        }
    }

    private static String getColumnName(
            Element element,
            String mainAlias,
            Map<String, EntityClassDto> entityClasses
    ) {
        return Optional.ofNullable(element.getAnnotation(CriteriaAttribute.class))
                .map(a ->
                        a.value().isEmpty() ?
                                getColumnName(mainAlias, element.getSimpleName().toString(), entityClasses) :
                                getColumnName(mainAlias, a, entityClasses)
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

    private static String getColumnListCode(CriteriaClassDto criteriaClassDto) {
        return criteriaClassDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getColumnName()))
                .map(field ->
                        (field.getColumnIndex() == 1 ? StringUtils.EMPTY : ",") +
                                "\n            \"" + field.getColumnName() + "\""
                )
                .reduce(
                        "    private static final List<String> COLUMNS = Arrays.asList(",
                        String::concat
                ) + "\n    );";
    }

    @Data
    private static class EntityAttribute {
        private String alias;
        private String fieldName;
        private EntityClassDto entityClass;
        private EntityFieldDto entityField;
    }
}