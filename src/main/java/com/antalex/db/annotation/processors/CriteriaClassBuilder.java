package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.*;
import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.criteria.CriteriaElement;
import com.antalex.db.model.criteria.CriteriaElementJoin;
import com.antalex.db.model.criteria.CriteriaPredicate;
import com.antalex.db.model.dto.*;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.impl.parser.SQLConditionParser;
import com.antalex.db.utils.Utils;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.persistence.criteria.JoinType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CriteriaClassBuilder {
    private static final Map<Element, CriteriaClassDto> CRITERIA_CLASSES = new HashMap<>();
    private static final Map<String, CriteriaJoinDto> JOINS = new HashMap<>();

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
            IntStream
                    .range(0, criteriaClassDto.getJoins().size())
                    .forEach(index -> criteriaClassDto.getJoins().get(index).setIndex(index + 1));
            if (!criteria.where().isBlank()) {
                Map<String, String> tokenMap = new HashMap<>();
                criteriaClassDto
                        .getFrom()
                        .getFields()
                        .forEach(it ->
                                tokenMap.put(
                                        it.getFieldName(),
                                        criteriaClassDto.getAlias() + "." + it.getColumnName()
                                ));
                entityClasses
                        .forEach((key, value) ->
                                value
                                        .getFields()
                                        .forEach(it ->
                                                tokenMap.put(
                                                        key + "." + it.getFieldName(),
                                                        key + "." + it.getColumnName()
                                                )
                                        )
                        );
                SQLConditionParser parser = new SQLConditionParser();
                criteriaClassDto.setPredicateGroups(
                        parser.getPredicateGroupsWithSimplifying(
                                parser.parse(
                                        Utils.transformCondition(criteria.where(), tokenMap)
                                )
                        )
                );
                if (
                        criteriaClassDto.getPredicateGroups().size() == 1 &&
                                Objects.nonNull(criteriaClassDto.getPredicateGroups().get(0).getValue())
                ) {
                    throw new IllegalArgumentException("Условие \"" + criteria.where() +
                            "\" всегда соответствует значению \"" +
                            criteriaClassDto.getPredicateGroups().get(0).getValue() + "\"");
                }
                if (!criteriaClassDto.getPredicateGroups().isEmpty()) {
                    Map<String, Integer> aliasIndexes =
                            criteriaClassDto.getJoins()
                                    .stream()
                                    .collect(Collectors.toMap(CriteriaJoinDto::getAlias, CriteriaJoinDto::getIndex));
                    aliasIndexes.put(criteriaClassDto.getAlias(), 0);

                    List<Set<String>> aliasSets = parser.getAliases();
                    criteriaClassDto.setPredicates(
                            parser.getPredicates()
                                    .entrySet()
                                    .stream()
                                    .sorted(Map.Entry.comparingByValue())
                                    .map(it ->
                                            new CriteriaPredicate()
                                                    .value(it.getKey())
                                                    .aliasMask(
                                                            aliasSets.get(it.getValue())
                                                                    .stream()
                                                                    .map(alias ->
                                                                            1L << getAliasIndexWithCheck(
                                                                                    alias,
                                                                                    aliasIndexes)
                                                                    )
                                                                    .reduce(0L, (a, b) -> a | b))
                                    ).toList()
                    );
                }
            }
            criteriaClassDto
                    .getJoins()
                    .forEach(join -> {
                        join.setColumns(entityColumns.get(join.getAlias()));
                        parseJoin(join, entityClasses);
                    });
            CRITERIA_CLASSES.put(classElement, criteriaClassDto);
        }
        return CRITERIA_CLASSES.get(classElement);
    }

    private static int getAliasIndexWithCheck(String alias, Map<String, Integer> aliasIndexes) {
        return Optional
                .ofNullable(aliasIndexes.get(alias))
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный псевдоним " + alias));
    }

    private static void parseJoin(CriteriaJoinDto joinDto, Map<String, EntityClassDto> entityClasses) {
        if (joinDto.getJoinType() == JoinType.RIGHT) {
            throw new IllegalArgumentException(
                    "Соединение типа RIGHT JOIN не поддерживается в распределенных запросах.");
        }
        JOINS.put(joinDto.getAlias(), joinDto);
        String on = joinDto.getOn().replaceAll("(\\r|\\n|\\t|\\s)", "");
        if (!Pattern.compile(PATTERN_JOIN_ON).matcher(on).matches()) {
            throw new IllegalArgumentException(
                    "Условие соединения '" + joinDto.getOn() + "' не соответствует шаблону: " + PATTERN_JOIN_ON);
        }
        int idx = on.indexOf("=");
        EntityAttribute entityAttribute = getEntityAttribute(on.substring(0, idx), joinDto.getAlias(), entityClasses);
        checkJoinAttribute(entityAttribute);
        EntityAttribute entityAttribute2 = getEntityAttribute(
                on.substring(idx + 1), joinDto.getAlias(), entityClasses);
        checkJoinAttribute(entityAttribute2);
        joinDto.setLinkedShard(
                Optional
                        .ofNullable(entityAttribute.getEntityField())
                        .map(EntityFieldDto::getElement)
                        .map(element -> ProcessorUtils.isAnnotationPresent(element, ParentShard.class))
                        .orElse(false)
                        ||
                        Optional
                                .ofNullable(entityAttribute2.getEntityField())
                                .map(EntityFieldDto::getElement)
                                .map(element -> ProcessorUtils.isAnnotationPresent(element, ParentShard.class))
                                .orElse(false)
        );
        if (joinDto.getAlias().equals(entityAttribute.getAlias())) {
            joinDto.setJoinColumns(
                    Pair.of(
                            getColumnName(entityAttribute, entityAttribute2),
                            getColumnName(entityAttribute2, entityAttribute)
                    )
            );
            joinDto.setJoinAlias(entityAttribute2.getAlias());
        } else if (joinDto.getAlias().equals(entityAttribute2.getAlias())) {
            joinDto.setJoinColumns(
                    Pair.of(
                            getColumnName(entityAttribute2, entityAttribute),
                            getColumnName(entityAttribute, entityAttribute2)
                    )
            );
            joinDto.setJoinAlias(entityAttribute.getAlias());
        } else {
            throw new IllegalArgumentException("Условие соединения " + joinDto.getOn() +
                    " не соответствует псевдониму " + joinDto.getAlias());
        }








        if (joinDto.getJoinType() == JoinType.INNER) {
            CriteriaJoinDto parentJoin = JOINS.get(joinDto.getJoinAlias());
            while (
                    Optional
                            .ofNullable(parentJoin)
                            .map(CriteriaJoinDto::getJoinType)
                            .map(joinType -> joinType == JoinType.LEFT)
                            .orElse(false)
            ) {
                parentJoin.setJoinType(JoinType.INNER);
                parentJoin = JOINS.get(parentJoin.getJoinAlias());
            }
        }
    }

    private static String getColumnName(EntityAttribute entityAttribute, EntityAttribute entityAttribute2) {
        return entityAttribute.getAlias() + "." +
                Optional
                        .ofNullable(entityAttribute.getEntityField())
                        .map(EntityFieldDto::getColumnName)
                        .orElseGet(() ->
                                Optional
                                        .ofNullable(entityAttribute2.getEntityField())
                                        .map(EntityFieldDto::getLinkedField)
                                        .map(EntityFieldDto::getColumnName)
                                        .orElse("ID")
                        );
    }

    private static EntityAttribute getEntityAttribute(
            String attributeName,
            String defaultAlias,
            Map<String, EntityClassDto> entityClasses)
    {
        int idx = attributeName.indexOf(".");
        EntityAttribute entityAttribute = new EntityAttribute();
        if (idx > 0) {
            entityAttribute.setAlias(attributeName.substring(0, idx).toUpperCase());
            entityAttribute.setFieldName(attributeName.substring(idx + 1));
        } else if (entityClasses.containsKey(attributeName.toUpperCase())) {
            entityAttribute.setAlias(attributeName.toUpperCase());
        } else {
            entityAttribute.setAlias(defaultAlias);
            entityAttribute.setFieldName(attributeName);
        }
        entityAttribute.setEntityClass(entityClasses.get(entityAttribute.getAlias()));
        if (
                Optional
                        .ofNullable(entityAttribute.getFieldName())
                        .map(fieldName -> !"ID".equalsIgnoreCase(fieldName))
                        .orElse(false)
        ) {
            entityAttribute.setEntityField(
                    Optional.ofNullable(entityAttribute.getEntityClass())
                            .map(EntityClassDto::getFieldMap)
                            .map(fieldMap -> fieldMap.get(entityAttribute.getFieldName()))
                            .orElse(null)
            );
            if (entityAttribute.getEntityField() != null && entityAttribute.getEntityField().getColumnName() == null) {
                throw new IllegalArgumentException("Для поля " + entityAttribute.getAlias() + "." +
                        entityAttribute.getFieldName() + " отсутствует соответствующая колонка в таблице.");
            }
        }
        return entityAttribute;
    }

    private static void checkJoinAttribute(EntityAttribute entityAttribute) {
        if (entityAttribute.getEntityClass() == null) {
            throw new IllegalArgumentException("Неизвестный псевдоним " + entityAttribute.getAlias());
        }
        if (
                entityAttribute.getEntityField() == null &&
                        Optional
                                .ofNullable(entityAttribute.getFieldName())
                                .map(fieldName -> !"ID".equalsIgnoreCase(entityAttribute.getFieldName()))
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
                .filter(alias -> columnName.toUpperCase().contains(alias + "."))
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
                                        " не соответствует классу сущности псевдонима " + alias);
                    }
                    return domainClassDto;
                })
                .orElse(null);
    }

    private static String getColumnName(String mainAlias,
                                        String attributeName,
                                        Map<String, EntityClassDto> entityClasses)
    {
        EntityAttribute entityAttribute = getEntityAttribute(attributeName, mainAlias, entityClasses);
        return Optional
                .ofNullable(entityAttribute.getEntityField())
                .map(EntityFieldDto::getColumnName)
                .map(columnName -> entityAttribute.getAlias() + "." + columnName)
                .orElseGet(() ->
                        Optional
                                .ofNullable(entityAttribute.getEntityClass())
                                .filter(it -> entityAttribute.getAlias().equals(attributeName.toUpperCase()))
                                .map(it -> entityAttribute.getAlias() + ".ID")
                                .orElse(attributeName)
                );
    }

    private static String getColumnName(
            Element element,
            String mainAlias,
            Map<String, EntityClassDto> entityClasses
    ) {
        return Optional.ofNullable(element.getAnnotation(CriteriaAttribute.class))
                .map(a ->
                                getColumnName(
                                        mainAlias,
                                        a.value().isEmpty() ? element.getSimpleName().toString() : a.value(),
                                        entityClasses)
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
                                            Component.class.getCanonicalName(),
                                            CriteriaRepository.class.getCanonicalName(),
                                            List.class.getCanonicalName(),
                                            Arrays.class.getCanonicalName(),
                                            Autowired.class.getCanonicalName(),
                                            ShardEntityManager.class.getCanonicalName(),
                                            ShardDataBaseManager.class.getCanonicalName(),
                                            CriteriaElement.class.getCanonicalName(),
                                            CriteriaElementJoin.class.getCanonicalName(),
                                            ShardType.class.getCanonicalName(),
                                            JoinType.class.getCanonicalName(),
                                            Pair.class.getCanonicalName(),
                                            Stream.class.getCanonicalName(),
                                            PredicateGroup.class.getCanonicalName(),
                                            CriteriaPredicate.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println("@Component");
            out.println("public class " +
                    className + " implements CriteriaRepository<" + criteriaClassDto.getTargetClassName() + "> {");
            out.println(getColumnListCode(criteriaClassDto));
            out.println();
            out.println(getElementsCode(criteriaClassDto));
            out.println();
            out.println(getElementListCode(criteriaClassDto));
            out.println();
            out.println(getPredicateGroupsCode(criteriaClassDto));
            out.println();
            out.println(getPredicateListCode(criteriaClassDto));
            out.println();
            out.println("    private final ShardDataBaseManager dataBaseManager;");
            out.println("    private final ShardEntityManager entityManager;");
            out.println();
            out.println(getConstructorCode(criteriaClassDto, className));
            out.println();
            out.println(getCode(criteriaClassDto));
            out.println("}");
        }
    }

    private static String getElementsCode(CriteriaClassDto criteriaClassDto) {
        return criteriaClassDto
                        .getJoins()
                        .stream()
                        .map(CriteriaClassBuilder::getElementsCode)
                        .reduce(
                                "    private static final CriteriaElement ELEMENT_" +
                                        criteriaClassDto.getAlias() +
                                        " = new CriteriaElement()\n" +
                                        "            .tableName(\""+ criteriaClassDto.getFrom().getTableName() +
                                        "\")\n" +
                                        "            .tableAlias(\"" + criteriaClassDto.getAlias() + "\")\n" +
                                        "            .shardType(ShardType." +
                                        criteriaClassDto.getFrom().getShardType().name() + ")\n" +
                                        "            .index(0)" +
                                        "            .columns(" + criteriaClassDto.getColumns() + "L);",
                                String::concat
                        );
    }

    private static String getElementsCode(CriteriaJoinDto criteriaJoinDto) {
        return "\n\n    private static final CriteriaElement ELEMENT_" + criteriaJoinDto.getAlias() +
                " = new CriteriaElement()\n" +
                "            .tableName(\""+ criteriaJoinDto.getFrom().getTableName() + "\")\n" +
                "            .tableAlias(\"" + criteriaJoinDto.getAlias() + "\")\n" +
                "            .shardType(ShardType." + criteriaJoinDto.getFrom().getShardType().name() + ")\n" +
                "            .index(" + criteriaJoinDto.getIndex() + ")" +
                "            .columns(" + criteriaJoinDto.getColumns() + "L)\n" +
                "            .join(\n" +
                "                    new CriteriaElementJoin()\n" +
                "                            .joinType(JoinType." + criteriaJoinDto.getJoinType().name() + ")\n" +
                "                            .linkedShard(" + criteriaJoinDto.getLinkedShard() + ")\n" +
                "                            .joinColumns(Pair.of(\"" + criteriaJoinDto.getJoinColumns().getLeft()
                + "\", \"" + criteriaJoinDto.getJoinColumns().getRight() + "\"))\n" +
                "                            .element(ELEMENT_" + criteriaJoinDto.getJoinAlias() + ")\n" +
                "            );";
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

    private static String getPredicateGroupsCode(CriteriaClassDto criteriaClassDto) {
        return IntStream.range(0, criteriaClassDto.getPredicateGroups().size())
                .mapToObj(idx ->
                        (idx == 0 ? StringUtils.EMPTY : ",") +
                                "\n            new PredicateGroup(" +
                                criteriaClassDto.getPredicateGroups().get(idx).getPredicateMask() + "L, " +
                                criteriaClassDto.getPredicateGroups().get(idx).getSignMask() + "L)"
                )
                .reduce(
                        "    private static final List<PredicateGroup> PREDICATE_GROUPS = Arrays.asList(",
                        String::concat
                ) + "\n    );";
    }

    private static String getPredicateListCode(CriteriaClassDto criteriaClassDto) {
        return IntStream.range(0, criteriaClassDto.getPredicates().size())
                .mapToObj(idx ->
                        (idx == 0 ? StringUtils.EMPTY : ",") +
                                "\n            new CriteriaPredicate()" +
                                "\n                    .value(\"" +
                                criteriaClassDto.getPredicates().get(idx).value() + "\")" +
                                "\n                    .aliasMask(" +
                                criteriaClassDto.getPredicates().get(idx).aliasMask() + "L)"
                )
                .reduce(
                        "    private static final List<CriteriaPredicate> PREDICATES = Arrays.asList(",
                        String::concat
                ) + "\n    );";
    }

    private static String getConstructorCode(CriteriaClassDto criteriaClassDto, String className) {
        return criteriaClassDto
                .getJoins()
                .stream()
                .map(join ->
                        "        ELEMENT_" + join.getAlias() + ".cluster(dataBaseManager.getCluster(\"" +
                                join.getFrom().getCluster() + "\"));\n")
                .reduce(
                        "    @Autowired\n" +
                                "    " + className + "(ShardEntityManager entityManager, ShardDataBaseManager dataBaseManager) {\n" +
                                "        this.dataBaseManager = dataBaseManager;\n" +
                                "        this.entityManager = entityManager;\n\n" +
                                "        ELEMENT_" + criteriaClassDto.getAlias() +
                                ".cluster(dataBaseManager.getCluster(\"" +
                                criteriaClassDto.getFrom().getCluster() + "\"));\n",
                        String::concat
                ) + "    }";
    }

    private static String getElementListCode(CriteriaClassDto criteriaClassDto) {
        return criteriaClassDto.getJoins()
                .stream()
                .map(join -> ",\n" + "            ELEMENT_" + join.getAlias()
                )
                .reduce(
                        "    private static final List<CriteriaElement> ELEMENTS = Arrays.asList(\n" +
                                "            ELEMENT_" + criteriaClassDto.getAlias(),
                        String::concat
                ) + "\n    );";
    }

    private static String getCode(CriteriaClassDto criteriaClassDto) {
        return "    @Override\n" +
                "    public Stream<" + criteriaClassDto.getTargetClassName() + "> get(Object... binds) {\n" +
                "        return null;\n" +
                "    }";
    }

    @Data
    private static class EntityAttribute {
        private String alias;
        private String fieldName;
        private EntityClassDto entityClass;
        private EntityFieldDto entityField;
    }
}