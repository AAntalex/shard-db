package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import com.google.common.base.CaseFormat;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.antalex.db.model.dto.EntityClassDto;
import com.antalex.db.model.dto.EntityFieldDto;
import com.antalex.db.model.dto.IndexDto;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.persistence.*;
import javax.sql.rowset.serial.SerialClob;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityClassBuilder {
    private static final Map<Element, EntityClassDto> ENTITY_CLASSES = new HashMap<>();

    private static List<IndexDto> getIndexes(Index[] indexes) {
        List<IndexDto> result = new ArrayList<>();
        for(Index index : indexes) {
            result.add(
                    IndexDto
                            .builder()
                            .name(index.name())
                            .unique(index.unique())
                            .columnList(index.columnList())
                            .build()
            );
        }
        return result;
    }

    public static EntityClassDto getClassDtoByElement(Element classElement) {
        ShardEntity shardEntity = classElement.getAnnotation(ShardEntity.class);
        if (shardEntity == null) {
            return null;
        }
        if (!ENTITY_CLASSES.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            boolean isFluent = Optional.ofNullable(classElement.getAnnotation(Accessors.class))
                    .map(Accessors::fluent)
                    .orElse(false);
            Map<String, String> getters = ProcessorUtils.getMethodsByPrefix(classElement, "get");
            Map<String, String> setters = ProcessorUtils.getMethodsByPrefix(classElement, "set");

            EntityClassDto entityClassDto = EntityClassDto
                    .builder()
                    .targetClassName(elementName)
                    .tableName(
                            Optional.ofNullable(classElement.getAnnotation(Table.class))
                                    .map(Table::name)
                                    .orElse(getTableName(shardEntity.tablePrefix(), classElement))
                    )
                    .chainAccessors(
                            Optional.ofNullable(classElement.getAnnotation(Accessors.class))
                                    .map(Accessors::chain)
                                    .orElse(false)
                    )
                    .classPackage(ProcessorUtils.getPackage(classElement.asType().toString()))
                    .cluster(shardEntity.cluster())
                    .shardType(shardEntity.type())
                    .classElement(classElement)
                    .fields(
                            ElementFilter.fieldsIn(classElement.getEnclosedElements())
                                    .stream()
                                    .map(
                                            fieldElement ->
                                                    EntityFieldDto
                                                            .builder()
                                                            .fieldName(fieldElement.getSimpleName().toString())
                                                            .columnName(
                                                                    getColumnName(
                                                                            shardEntity.columnPrefix(),
                                                                            fieldElement
                                                                    )
                                                            )
                                                            .isLinked(isLinkedField(fieldElement))
                                                            .getter(
                                                                    ProcessorUtils.
                                                                            findGetter(getters, fieldElement, isFluent)
                                                            )
                                                            .setter(
                                                                    ProcessorUtils.
                                                                            findSetter(setters, fieldElement, isFluent)
                                                            )
                                                            .element(fieldElement)
                                                            .build()
                                    )
                                    .collect(Collectors.toList())
                    )
                    .indexes(
                            Optional.ofNullable(classElement.getAnnotation(Table.class))
                                    .map(Table::indexes)
                                    .map(EntityClassBuilder::getIndexes)
                                    .orElse(Collections.emptyList())
                    )
                    .build();

            normalizeClassDto(entityClassDto);
            ENTITY_CLASSES.put(classElement, entityClassDto);
        }
        return ENTITY_CLASSES.get(classElement);
    }

    private static void normalizeClassDto(EntityClassDto entityClassDto) {
        entityClassDto.setFieldMap(entityClassDto.getFields()
                .stream()
                .collect(Collectors.toMap(EntityFieldDto::getFieldName, it -> it)));

        entityClassDto
                .getIndexes()
                .stream()
                .filter(IndexDto::getUnique)
                .map(IndexDto::getColumnList)
                .map(columnList -> columnList.replace(StringUtils.SPACE, StringUtils.EMPTY))
                .forEach(fields -> {
                    for (String fieldName : fields.split(",")) {
                        EntityFieldDto entityFieldDto = entityClassDto.getFieldMap().get(fieldName);
                        if (Objects.nonNull(entityFieldDto) &&
                                !"id".equals(fieldName) &&
                                Objects.nonNull(entityFieldDto.getGetter()) &&
                                Objects.nonNull(entityFieldDto.getColumnName()) &&
                                !entityFieldDto.getIsLinked())
                        {
                            entityFieldDto.setUnique(true);
                        }
                    }
                });
        entityClassDto.setColumnFields(
                entityClassDto.getFields()
                        .stream()
                        .filter(field -> !field.getIsLinked() && Objects.nonNull(field.getColumnName()))
                        .collect(Collectors.toList())
        );
        IntStream.range(0, entityClassDto.getColumnFields().size())
                .forEach(idx -> entityClassDto.getColumnFields().get(idx).setColumnIndex(idx + 1));
        entityClassDto.setUniqueFields(
                entityClassDto.getColumnFields()
                        .stream()
                        .filter(EntityFieldDto::isUnique)
                        .collect(Collectors.toList())
        );
    }

    private static boolean isLinkedField(Element element) {
        return ProcessorUtils.isAnnotationPresent(element, OneToMany.class)
                && ProcessorUtils.isAnnotationPresent(element, JoinColumn.class);
    }


    private static String getColumnName(String columnPrefix, Element element) {
        if (ProcessorUtils.isAnnotationPresent(element, Transient.class)) {
            return null;
        }
        String columnName =
                Optional.ofNullable(element.getAnnotation(Column.class))
                        .map(Column::name)
                        .orElse(
                                Optional.ofNullable(element.getAnnotation(JoinColumn.class))
                                        .map(JoinColumn::name)
                                        .orElse(StringUtils.EMPTY)
                        );
        return StringUtils.isEmpty(columnName) ?
                columnPrefix +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, element.getSimpleName().toString()) :
                columnName;
    }

    private static String getTableName(String tablePrefix, Element element) {
        return tablePrefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
                element.getSimpleName().toString());
    }

    private static EntityFieldDto findFieldByLinkedColumn(EntityFieldDto field) {
        EntityClassDto fieldClass = getClassDtoByElement(
                (
                        (DeclaredType) ProcessorUtils.getDeclaredType(field.getElement()).getTypeArguments().get(0)
                ).asElement()
        );
        if (fieldClass == null || fieldClass.getFields() == null) {
            return null;
        }
        return fieldClass.getFields()
                .stream()
                .filter(it ->
                        field.getColumnName().equals(
                                Optional.ofNullable(it.getColumnName()).orElse(StringUtils.EMPTY)
                        ) &&
                                (
                                        ProcessorUtils.isAnnotationPresentByType(it.getElement(), ShardEntity.class) ||
                                                ProcessorUtils.getClassByType(it.getElement().asType()) == Long.class
                                )
                )
                .filter(it -> Objects.nonNull(it.getSetter()))
                .findFirst()
                .orElse(null);
    }


    private static Long getUniqueColumnsValueCode(EntityClassDto entityClassDto) {
        long ret = 0L;
        for (int i = 0; i < entityClassDto.getColumnFields().size(); i++) {
            if (entityClassDto.getColumnFields().get(i).isUnique() && i <= Long.SIZE) {
                ret = ret | 1L << i;
            }
        }
        return ret;
    }

    private static String getInsertSQLCode(EntityClassDto entityClassDto, boolean unique) {
        String columns = "SN,ST,SHARD_MAP";
        String values = "0,?,?";
        for (EntityFieldDto field : unique ? entityClassDto.getUniqueFields() : entityClassDto.getColumnFields()) {
            if (Objects.nonNull(field.getGetter())) {
                columns = columns.concat(",").concat(field.getColumnName());
                values = values.concat(",?");
            }
        }
        return "INSERT INTO $$$." + entityClassDto.getTableName() + " (" + columns + ",ID) VALUES (" + values + ",?)";
    }

    private static String getUpdateSQLCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field -> "," + field.getColumnName() + "=?")
                .reduce(
                        "UPDATE $$$." + entityClassDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?",
                        String::concat
                ) + " WHERE ID=?";
    }

    private static String getSelectPrefixCode(EntityClassDto entityClassDto) {
        StringBuilder code = new StringBuilder("SELECT ").append(getSelectList(entityClassDto, "x0"));
        int idx = 0;
        for (EntityFieldDto field : entityClassDto.getColumnFields()) {
            if (isEagerField(field)) {
                EntityClassDto entityClassDtoField = getClassDtoByElement(
                        ProcessorUtils.getDeclaredType(field.getElement()).asElement()
                );
                if (Objects.nonNull(entityClassDtoField)) {
                    idx++;
                    code
                            .append(",")
                            .append(getSelectList(entityClassDtoField, "x" + idx));
                }
            }
        }
        return code.toString();
    }

    private static String getFromPrefixCode(EntityClassDto entityClassDto) {
        StringBuilder fromCode = new StringBuilder(" FROM $$$.")
                .append(entityClassDto.getTableName())
                .append(" x0");
        int idx = 0;
        for (EntityFieldDto field : entityClassDto.getColumnFields()) {
            if (isEagerField(field)) {
                EntityClassDto entityClassDtoField = getClassDtoByElement(
                        ProcessorUtils.getDeclaredType(field.getElement()).asElement()
                );
                if (Objects.nonNull(entityClassDtoField)) {
                    idx++;
                    fromCode
                            .append(" LEFT OUTER JOIN $$$.")
                            .append(entityClassDtoField.getTableName())
                            .append(" x")
                            .append(idx)
                            .append(" ON x0.")
                            .append(field.getColumnName())
                            .append(" = x")
                            .append(idx)
                            .append(".ID");
                }
            }
        }
        return fromCode.toString();
    }

    private static String getSelectList(EntityClassDto entityClassDto, String alias) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field -> "," + alias + "." + field.getColumnName())
                .reduce(alias + ".ID," + alias + ".SHARD_MAP", String::concat);
    }

    private static String getLockSQLCode(EntityClassDto entityClassDto) {
        return "SELECT ID FROM $$$." + entityClassDto.getTableName() + " WHERE ID=? FOR UPDATE NOWAIT";
    }

    public static void createRepositoryClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        EntityClassDto entityClassDto = getClassDtoByElement(annotatedElement);
        if (entityClassDto == null) {
            return;
        }

        String className = entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_REPOSITORY_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + entityClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    "import " + entityClassDto.getClassPackage() + "." +
                            entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX + ";"
            );
            out.println(
                    getImportedTypes(
                            entityClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            QueryType.class.getCanonicalName(),
                                            QueryStrategy.class.getCanonicalName(),
                                            ShardEntityRepository.class.getCanonicalName(),
                                            ShardEntityManager.class.getCanonicalName(),
                                            Component.class.getCanonicalName(),
                                            Autowired.class.getCanonicalName(),
                                            ShardType.class.getCanonicalName(),
                                            Cluster.class.getCanonicalName(),
                                            ShardDataBaseManager.class.getCanonicalName(),
                                            StorageContext.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ResultQuery.class.getCanonicalName(),
                                            List.class.getCanonicalName(),
                                            ArrayList.class.getCanonicalName(),
                                            StringUtils.class.getCanonicalName(),
                                            IntStream.class.getCanonicalName(),
                                            Objects.class.getCanonicalName(),
                                            Arrays.class.getCanonicalName(),
                                            Map.class.getCanonicalName(),
                                            HashMap.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName(),
                                            SerialClob.class.getCanonicalName(),
                                            AttributeStorage.class.getCanonicalName(),
                                            DataStorage.class.getCanonicalName(),
                                            FetchType.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println();
            out.println("@Component");
            out.println("public class " +
                    className +
                    " implements ShardEntityRepository<" +
                    entityClassDto.getTargetClassName() + "> {"
            );
            out.println(
                    "    private static final ShardType SHARD_TYPE = ShardType." +
                            entityClassDto.getShardType().name() + ";"
            );

            out.println(
                    "    private static final String UPD_QUERY_PREFIX = \"UPDATE $$$." +
                            entityClassDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?\";"
            );
            out.println(
                    "    private static final String INS_QUERY = \"" + getInsertSQLCode(entityClassDto, false) +
                            "\";"
            );
            out.println(
                    "    private static final String UPD_QUERY = \"" + getUpdateSQLCode(entityClassDto) + "\";"
            );
            if (!entityClassDto.getUniqueFields().isEmpty()) {
                out.println(
                        "    private static final String INS_UNIQUE_FIELDS_QUERY = \"" +
                                getInsertSQLCode(entityClassDto, true) + "\";"
                );
            }
            out.println(
                    "    private static final String DELETE_QUERY = \"DELETE FROM $$$." +
                            entityClassDto.getTableName() + " WHERE ID=?\";"
            );
            out.println(
                    "    private static final String LOCK_QUERY = \"" + getLockSQLCode(entityClassDto) + "\";"
            );
            out.println(
                    "    private static final String SELECT_PREFIX = \"" + getSelectPrefixCode(entityClassDto) + "\";"
            );
            out.println(
                    "    private static final String FROM_PREFIX = \"" + getFromPrefixCode(entityClassDto) + "\";"
            );
            if (!entityClassDto.getUniqueFields().isEmpty()) {
                out.println(
                        "    private static final Long UNIQUE_COLUMNS = " +
                                getUniqueColumnsValueCode(entityClassDto) + "L;"
                );
            }
            out.println();
            out.println(getColumnsCode(entityClassDto));
            out.println("    private Map<Long, String> updateQueries = new HashMap<>();");


            out.println();
            out.println("    private ShardEntityManager entityManager;");
            out.println("    private final Cluster cluster;");

            out.println();
            out.println(getConstructorCode(entityClassDto, className));
            out.println();
            out.println(getSetEntityManagerCode());
            out.println();
            out.println(getNewEntityCode(entityClassDto));
            out.println();
            out.println(getShardTypeCode(entityClassDto));
            out.println();
            out.println(getClusterCode(entityClassDto));
            out.println();
            out.println(getSetDependentStorageCode(entityClassDto));
            out.println();
            out.println(getPersistCode(entityClassDto));
            out.println();
            out.println(getGenerateDependentIdCode(entityClassDto));
            out.println();
            out.println(getLockCode(entityClassDto));
            out.println();
            out.println(getExtractValuesCode(entityClassDto));
            out.println();
            out.println(getFindCode(entityClassDto));
            out.println();
            out.println(getFindAllCode(entityClassDto));
            out.println();
            out.println(getSkipLockedCode(entityClassDto));
            out.println();
            out.println(getFindAllParentCode(entityClassDto));
            out.println();
            out.println(getAdditionalPersistCode(entityClassDto));
            out.println();
            out.println(getFindAllPrivateCode(entityClassDto));
            out.println();
            out.println(getMethodUpdateSQLCode());
            out.println();
            out.println(getSelectQueryCode());
            out.println("}");
        }
    }

    public static void createInterceptorClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        EntityClassDto entityClassDto = getClassDtoByElement(annotatedElement);
        if (entityClassDto == null) {
            return;
        }
        String className = entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + entityClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    getImportedTypes(
                            entityClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            ShardEntityManager.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println("public class " + className + " extends " + entityClassDto.getTargetClassName() + " {");
            out.println("""
                        private ShardEntityManager entityManager;
                        public void setEntityManager(ShardEntityManager entityManager) {
                            this.entityManager = entityManager;
                        }
                    """);
            out.println(getLazyFlagsCode(entityClassDto));
            out.println(getInitCode(entityClassDto));
            out.println(getLazyFlagMethodCode(entityClassDto));
            out.println(getGettersCode(entityClassDto));
            out.println(getSettersCode(entityClassDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(EntityClassDto entityClassDto, List<String> importedTypes) {
        entityClassDto.getFields()
                .stream()
                .map(EntityFieldDto::getElement)
                .filter(element ->
                        !ProcessorUtils.isAnnotationPresent(element, Transient.class))
                .map(ProcessorUtils::getDeclaredType)
                .forEach(type -> {
                    importedTypes.add(type.asElement().toString());
                    if (!type.getTypeArguments().isEmpty()) {
                        type.getTypeArguments()
                                .stream()
                                .map(it -> (DeclaredType) it)
                                .map(DeclaredType::asElement)
                                .forEach(element ->
                                        importedTypes.add(element.toString())
                                );
                    }
                });
        return ProcessorUtils.getImportedTypes(importedTypes);
    }

    private static boolean isLazyList(EntityFieldDto field) {
        return Optional.ofNullable(field)
                .map(EntityFieldDto::getElement)
                .map(it -> it.getAnnotation(OneToMany.class))
                .map(OneToMany::fetch)
                .filter(it -> it == FetchType.LAZY)
                .isPresent();
    }

    private static boolean isEagerField(EntityFieldDto field) {
        return Optional.ofNullable(field)
                .map(EntityFieldDto::getElement)
                .map(it -> it.getAnnotation(OneToOne.class))
                .map(OneToOne::fetch)
                .filter(it -> it == FetchType.EAGER)
                .isPresent();
    }

    private static String getLazyFlagsCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !ProcessorUtils.isAnnotationPresent(field.getElement(), Transient.class)
                )
                .map(field -> "    private boolean " + field.getFieldName() + "Lazy = false;\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getInitCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !ProcessorUtils.isAnnotationPresent(field.getElement(), Transient.class)
                )
                .map(field ->
                        "\n        this." + field.getFieldName() + "Lazy = true;" +
                                (
                                        !isLazyList(field) ?
                                                "\n        if (!this.isLazy()) {" +
                                                "\n            this." + field.getSetter() + "(entityManager.findAll(" +
                                                        ProcessorUtils.getFinalType(field.getElement()) + ".class, " +
                                                        (
                                                                ProcessorUtils.isAnnotationPresent(
                                                                        field.getElement(),
                                                                        ParentShard.class
                                                                ) ? "this, " : StringUtils.EMPTY
                                                        ) + "\"x0." + field.getColumnName() + "=?\", this.id));\n" +
                                                        "            this." + field.getFieldName() + "Lazy = false;\n" +
                                                        "        }" :
                                                StringUtils.EMPTY
                                )
                )
                .reduce("    public void init() {", String::concat) + "\n    }";
    }

    private static String getLazyFlagMethodCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !ProcessorUtils.isAnnotationPresent(field.getElement(), Transient.class)
                )
                .map(field ->
                        "\n    public boolean " + field.getFieldName() + "IsLazy() {\n" +
                                "        return this." + field.getFieldName() + "Lazy;\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getGettersCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        !ProcessorUtils.isAnnotationPresent(field.getElement(), Transient.class) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                        "\n    @Override\n" +
                                "    public " + ProcessorUtils.getTypeField(field.getElement()) + " " +
                                field.getGetter() + "() {\n" +
                                (
                                        field.getIsLinked() &&
                                                ProcessorUtils.isAnnotationPresentInArgument(
                                                        field.getElement(),
                                                        ShardEntity.class
                                                ) ?
                                                "        if (" + field.getFieldName() + "Lazy) {\n" +
                                                        "            this." + field.getSetter() +
                                                        "(entityManager.findAll(" +
                                                        ProcessorUtils.getFinalType(field.getElement()) + ".class, " +
                                                        (
                                                                ProcessorUtils.isAnnotationPresent(
                                                                        field.getElement(),
                                                                        ParentShard.class
                                                                ) ?
                                                                        "this, " :
                                                                        StringUtils.EMPTY
                                                        ) +
                                                        "\"x0." +
                                                        field.getColumnName() + "=?\", this.id));\n" +
                                                        "            this." + field.getFieldName() +
                                                        "Lazy = false;\n" +
                                                        "        }\n" :
                                                """
                                                                if (this.isLazy()) {
                                                                    entityManager.find(this);
                                                                }
                                                        """
                                ) +
                                "        return super." + field.getGetter() + "();\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(EntityClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "\n    public " + (classDto.getChainAccessors() ? classDto.getTargetClassName() : "void") +
                                " " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) +
                                " value, boolean change) {\n" +
                                "        if (change) {\n" +
                                "            if (this.isLazy()) {\n" +
                                "                entityManager.find(this);\n" +
                                "            }\n" +
                                "            this.setChanges(" + field.getColumnIndex() + ");\n" +
                                "        }\n" +
                                "        " + (classDto.getChainAccessors() ? "return " : StringUtils.EMPTY) +
                                "super." + field.getSetter() + "(value);\n" +
                                "    }\n" +
                                "    @Override\n" +
                                "    public " +
                                (classDto.getChainAccessors() ? classDto.getTargetClassName() : "void") +
                                " " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) + " value) {\n" +
                                "        " + (classDto.getChainAccessors() ? "return " : StringUtils.EMPTY) +
                                field.getSetter() + "(value, true);\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getColumnsCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .map(field ->
                        (field.getColumnIndex() == 1 ? StringUtils.EMPTY : ",") +
                                "\n            \"" + field.getColumnName() + "\""
                )
                .reduce(
                        "    private static final List<String> COLUMNS = Arrays.asList(",
                        String::concat
                ) + "\n    );";
    }

    private static String getConstructorCode(EntityClassDto entityClassDto, String className) {
        return "    @Autowired\n" +
                "    " + className + "(ShardDataBaseManager dataBaseManager) {\n" +
                "        this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + entityClassDto.getCluster() +
                "\"));\n    }";
    }

    private static String getMethodUpdateSQLCode() {
        return """
                    private String getUpdateSQL(Long changes) {
                        if (
                                Optional.ofNullable(changes)
                                .map(it -> it.equals(0L) && COLUMNS.size() <= Long.SIZE)
                                .orElse(true))\s
                        {
                            return null;
                        }
                        String sql = updateQueries.get(changes);
                        if (Objects.isNull(sql)) {
                            sql = IntStream.range(0, COLUMNS.size())
                                    .filter(idx -> idx > Long.SIZE || (changes & (1L << idx)) > 0L)
                                    .mapToObj(idx -> "," + COLUMNS.get(idx) + "=?")
                                    .reduce(UPD_QUERY_PREFIX, String::concat) + " WHERE ID=?";
                            updateQueries.put(changes, sql);
                        }
                        return sql;
                    }\
                """;
    }

    private static String getSelectQueryCode() {
        return """
                    private String getSelectQuery(Map<String, DataStorage> storageMap) {
                        if (Objects.nonNull(storageMap)) {
                            StringBuilder selectPrefix = new StringBuilder(SELECT_PREFIX);
                            StringBuilder fromPrefix = new StringBuilder(FROM_PREFIX);
                            int idx = 0;
                            for (DataStorage dataStorage : storageMap.values()) {
                            if (
                                    dataStorage.getFetchType() == FetchType.EAGER &&
                                            Optional.ofNullable(dataStorage.getCluster())
                                                    .map(it -> it == cluster)
                                                    .orElse(true)
                            ) {
                                    idx++;
                                    selectPrefix
                                            .append(",s").append(idx)
                                            .append(".ID,s").append(idx)
                                            .append(".SHARD_MAP,s").append(idx)
                                            .append(".C_ENTITY_ID,s").append(idx)
                                            .append(".C_STORAGE_NAME,s").append(idx)
                                            .append(".C_DATA,s").append(idx)
                                            .append(".C_DATA_FORMAT");
                                    fromPrefix
                                            .append(" LEFT OUTER JOIN $$$.APP_ATTRIBUTE_STORAGE s").append(idx)
                                            .append(" ON s").append(idx)
                                            .append(".C_ENTITY_ID=x0.ID and s").append(idx)
                                            .append(".C_STORAGE_NAME='").append(dataStorage.getName()).\
                append("'");
                                }
                            }
                            return selectPrefix + fromPrefix.toString() + " WHERE x0.SHARD_MAP>=0";
                        } else {
                            return SELECT_PREFIX + FROM_PREFIX + " WHERE x0.SHARD_MAP>=0";
                        }
                    }
                """;
    }

    private static String getNewEntityCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " newEntity() {\n" +
                "        return new " + entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX +
                "();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " getEntity(Long id, StorageContext" +
                " storageContext) {\n" +
                "        " + entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX +
                " entity = new " +
                entityClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX + "();\n" +
                "        entity.setId(id);\n" +
                "        entity.setStorageContext(storageContext);\n" +
                "        entity.setEntityManager(entityManager);\n" +
                "        entity.init();\n" +
                "        return entity;\n" +
                "    }";
    }

    private static String getFindCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " find(" + entityClassDto.getTargetClassName() +
                " entity, Map<String, DataStorage> storageMap) {\n" +
                "        try {\n" +
                "            ResultQuery result = entityManager\n" +
                "                    .createQuery(\n" +
                "                            entity,\n" +
                "                            getSelectQuery(storageMap) + \" and x0.ID=?\",\n" +
                "                            QueryType.SELECT,\n" +
                "                            QueryStrategy.OWN_SHARD\n" +
                "                    )\n" +
                "                    .bind(entity.getId())\n" +
                "                    .getResult();\n" +
                "            if (result.next()) {\n" +
                getProcessResultCode(entityClassDto) +
                "            } else {\n" +
                "                return null;\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entity;\n" +
                "    }";
    }

    private static String getFindAllCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public List<" + entityClassDto.getTargetClassName() +
                ">  findAll(\n" +
                "            Map<String, DataStorage> storageMap,\n" +
                "            Integer limit,\n" +
                "            String condition,\n" +
                "            Object... binds)\n" +
                "    {\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                " + entityClassDto.getTargetClassName() + ".class, \n" +
                "                                getSelectQuery(storageMap) +\n" +
                "                                        Optional.ofNullable(condition).map(it -> \" and \" + it)" +
                ".orElse(StringUtils.EMPTY),\n" +
                "                                QueryType.SELECT\n" +
                "                        )\n" +
                "                        .bindAll(binds)\n" +
                "                        .getResult(),\n" +
                "                storageMap\n" +
                "        );\n" +
                "    }";
    }

    private static String getSkipLockedCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public List<" + entityClassDto.getTargetClassName() + "> skipLocked(\n" +
                "            Integer limit,\n" +
                "            String condition,\n" +
                "            Object... binds) {\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                " + entityClassDto.getTargetClassName() + ".class,\n" +
                "                                getSelectQuery(null) +\n" +
                "                                        Optional.ofNullable(condition)\n" +
                "                                                .map(it -> \" and \" + it)\n" +
                "                                                .orElse(StringUtils.EMPTY) +\n" +
                "                                \" FOR UPDATE SKIP LOCKED\",\n" +
                "                                QueryType.LOCK\n" +
                "                        )\n" +
                "                        .fetchLimit(limit)\n" +
                "                        .bindAll(binds)\n" +
                "                        .getResult(),\n" +
                "                null\n" +
                "        );\n" +
                "    }";
    }

    private static String getFindAllParentCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public List<" + entityClassDto.getTargetClassName() +
                "> findAll(\n" +
                "            ShardInstance parent,\n" +
                "            Map<String, DataStorage> storageMap,\n" +
                "            String condition,\n" +
                "            Object... binds)\n" +
                "    {\n" +
                "        if (parent.getStorageContext().getCluster() != this.cluster) {\n" +
                "            return findAll(storageMap, null, condition, binds);\n" +
                "        }\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                parent,\n" +
                "                                getSelectQuery(storageMap) +\n" +
                "                                        Optional.ofNullable(condition).map(it -> \" and \" + it)" +
                ".orElse(StringUtils.EMPTY),\n" +
                "                                QueryType.SELECT\n" +
                "                        )\n" +
                "                        .bindAll(binds)\n" +
                "                        .getResult(),\n" +
                "                storageMap\n" +
                "        );\n" +
                "    }";
    }

    private static String getFindAllPrivateCode(EntityClassDto entityClassDto) {
        return  "    private List<" + entityClassDto.getTargetClassName() +
                "> findAll(ResultQuery result, Map<String, DataStorage> storageMap) {\n" +
                "        List<" + entityClassDto.getTargetClassName() + "> entities = new ArrayList<>();\n" +
                "        try {\n" +
                "            while (result.next()) {\n" +
                "                " + entityClassDto.getTargetClassName() +
                " entity = entityManager.getEntity(" + entityClassDto.getTargetClassName() +
                ".class, result.getLong(1));\n" +
                getProcessResultCode(entityClassDto) +
                "                entities.add(entity);\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entities;\n" +
                "    }";
    }

    private static int getCountSelectColumns(EntityClassDto entityClassDto) {
        if (entityClassDto == null) {
            return 0;
        }
        return (int) entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .count() + 2;
    }

    private static String getProcessResultCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()) && isEagerField(it))
                .map(field ->
                        "                if (\n" +
                                "                        result.getLong(index+1) != 0L &&\n" +
                                "                                Optional.ofNullable(entity." + field.getGetter() +
                                "()).map(ShardInstance::isLazy).orElse(false))\n" +
                                "                {\n" +
                                "                    entityManager.extractValues(entity." + field.getGetter() +
                                "(), result, index);\n" +
                                "                    index = index + " +
                                getCountSelectColumns(
                                        getClassDtoByElement(
                                                ProcessorUtils.getDeclaredType(field.getElement()).asElement()
                                        )
                                ) + ";\n" +
                                "                }\n"
                )
                .reduce(
                        "                int index = 0;\n" +
                                "                extractValues(entity, result, index);\n" +
                                "                index = index + " + getCountSelectColumns(entityClassDto) + ";\n",
                        String::concat
                ) +
                "                entity.setAttributeStorage(entityManager.extractAttributeStorage" +
                "(storageMap, result, cluster, index));\n";
    }

    private static String getResultObjectCode(EntityFieldDto field) {
        Class<?> clazz = ProcessorUtils.getClassByType(field.getElement().asType());
        if (clazz != null) {
            if (clazz.isAssignableFrom(String.class)) {
                return "result.getString(++index)";
            }
            if (clazz.isAssignableFrom(Byte.class)) {
                return "result.getByte(++index)";
            }
            if (clazz.isAssignableFrom(Boolean.class)) {
                return "result.getBoolean(++index)";
            }
            if (clazz.isAssignableFrom(Short.class)) {
                return "result.getShort(++index)";
            }
            if (clazz.isAssignableFrom(Integer.class)) {
                return "result.getInteger(++index)";
            }
            if (clazz.isAssignableFrom(Long.class)) {
                return "result.getLong(++index)";
            }
            if (clazz.isAssignableFrom(Float.class)) {
                return "result.getFloat(++index)";
            }
            if (clazz.isAssignableFrom(Double.class)) {
                return "result.getDouble(++index)";
            }
            if (clazz.isAssignableFrom(BigDecimal.class)) {
                return "result.getBigDecimal(++index)";
            }
            if (clazz.isAssignableFrom(Date.class)) {
                return "result.getDate(++index)";
            }
            if (clazz.isAssignableFrom(Time.class)) {
                return "result.getTime(++index)";
            }
            if (clazz.isAssignableFrom(Timestamp.class)) {
                return "result.getTimestamp(++index)";
            }
            if (clazz.isAssignableFrom(Blob.class)) {
                return "result.getBlob(++index)";
            }
            if (clazz.isAssignableFrom(Clob.class)) {
                return "result.getClob(++index)";
            }
            if (clazz.isAssignableFrom(URL.class)) {
                return "result.getURL(++index)";
            }
            if (clazz.isAssignableFrom(SQLXML.class)) {
                return "result.getSQLXML(++index)";
            }
            if (clazz.isAssignableFrom(LocalDateTime.class)) {
                return "result.getLocalDateTime(++index)";
            }
            if (clazz.isAssignableFrom(LocalDate.class)) {
                return "result.getLocalDate(++index)";
            }
        }
        return "result.getObject(++index, " + ProcessorUtils.getTypeField(field.getElement()) + ".class)";
    }

    private static String getExtractValuesCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "                entityInterceptor." + field.getSetter() +
                                (
                                        ProcessorUtils.isAnnotationPresentByType(
                                                field.getElement(),
                                                ShardEntity.class
                                        ) ?
                                                "(entityManager.getEntity(" +
                                                        ProcessorUtils.getTypeField(field.getElement()) +
                                                        ".class, result.getLong(++index)), false);\n" :
                                        "(" + getResultObjectCode(field) + ", false);\n"
                                )
                )
                .reduce(
                        "    @Override\n" +
                                "    public " + entityClassDto.getTargetClassName() + " extractValues(" +
                                entityClassDto.getTargetClassName() + " entity, " +
                                "ResultQuery result, int index) {\n" +
                                "        try {\n" +
                                "            if (!Optional.ofNullable(result.getLong(++index)).map(it -> it == 0L)" +
                                ".orElse(true)) {\n" +
                                "                " + entityClassDto.getTargetClassName() +
                                ProcessorUtils.CLASS_INTERCEPT_POSTFIX +
                                " entityInterceptor =\n" +
                                "                        (" + entityClassDto.getTargetClassName() +
                                ProcessorUtils.CLASS_INTERCEPT_POSTFIX + ") Optional.ofNullable(entity)\n" +
                                "                                .orElse(entityManager.getEntity(" +
                                entityClassDto.getTargetClassName() + ".class, result.getLong(index)));\n" +
                                "                entityInterceptor.setShardMap(result.getLong(++index));\n",
                        String::concat
                ) +
                "                entityInterceptor.getStorageContext().setLazy(false);\n" +
                "                entityInterceptor.init();\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return null;\n" +
                "    }";
    }

    private static String getPersistCode(EntityClassDto entityClassDto) {
        StringBuilder code = new StringBuilder(
                "    @Override\n" +
                        "    public void persist(" + entityClassDto.getTargetClassName() +
                        " entity, boolean delete, boolean onlyChanged) {\n" +
                        "        if (delete) {\n" +
                        entityClassDto
                                .getFields()
                                .stream()
                                .filter(field ->
                                        Objects.nonNull(field.getGetter()) &&
                                                ProcessorUtils.isAnnotationPresentInArgument(
                                                        field.getElement(), ShardEntity.class
                                                )
                                )
                                .map(field ->
                                        "            entityManager.persistAll(entity." +
                                                field.getGetter() + "(), true, false);\n"
                                )
                                .reduce(StringUtils.EMPTY, String::concat) +
                        (
                                entityClassDto.getUniqueFields().isEmpty() ?
                                        StringUtils.EMPTY :
                                        """
                                                            if (!entity.hasMainShard()) {
                                                                entityManager.createQuery(entity, DELETE_QUERY, QueryType.DML, QueryStrategy.MAIN_SHARD)
                                                                        .bind(entity.getId())
                                                                        .addBatch();
                                                            }
                                                """
                        ) +
                        "            entityManager\n" +
                        "                    .createQueries(entity, DELETE_QUERY, QueryType.DML)\n" +
                        "                    .forEach(query -> query.bind(entity.getId()).addBatch());\n" +
                        "        } else {\n"
        );
        StringBuilder persistCode =
                new StringBuilder(
                        """            
                                       String sql = entity.isStored() ? (onlyChanged ? getUpdateSQL(entity.getChanges()) : UPD_QUERY) : INS_QUERY;
                                       if (Objects.nonNull(sql)) {
                                           boolean checkChanges = onlyChanged && entity.isStored();
                                           entityManager
                                                   .createQueries(entity, sql, QueryType.DML)
                                                   .forEach(query ->
                                                           query
                                                                   .bind(entityManager.getTransactionUUID())
                                                                   .bindShardMap(entity)
                           """
                );
        StringBuilder childPersistCode = new StringBuilder();
        for (EntityFieldDto field : entityClassDto.getFields()) {
            if (Objects.nonNull(field.getGetter())) {
                if (ProcessorUtils.isAnnotationPresentByType(field.getElement(), ShardEntity.class)) {
                    code
                            .append("            entityManager.persist(entity.")
                            .append(field.getGetter())
                            .append("(), onlyChanged);\n");
                }
                if (ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), ShardEntity.class)) {
                    childPersistCode
                            .append("            if (!((")
                            .append(entityClassDto.getTargetClassName())
                            .append(ProcessorUtils.CLASS_INTERCEPT_POSTFIX)
                            .append(") entity).")
                            .append(field.getFieldName())
                            .append("IsLazy()) {\n")
                            .append("                entityManager.persistAll(entity.")
                            .append(field.getGetter())
                            .append("(), false, onlyChanged);\n")
                            .append("            }\n");
                }
                if (field.getColumnIndex() > 0) {
                    persistCode
                            .append("                                        .bind(entity.")
                            .append(field.getGetter())
                            .append(
                                    ProcessorUtils.isAnnotationPresentByType(field.getElement(), ShardEntity.class) ?
                                            "().getId()" :
                                            "()"
                            )
                            .append(", checkChanges && !entity.isChanged(")
                            .append(field.getColumnIndex())
                            .append("))\n");
                }
            }
        }
        return code
                .append(persistCode)
                .append(
                        """
                                                                        .bind(entity.getId())
                                                                        .addBatch()
                                                        );
                                            }
                                            additionalPersist(entity);
                                """
                )
                .append(childPersistCode)
                .append("        }\n")
                .append("    }")
                .toString();
    }

    private static String getAdditionalPersistCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(field -> Objects.nonNull(field.getGetter()))
                .map(field ->
                        "                                    .bind(entity." +
                                field.getGetter() +
                                (
                                        ProcessorUtils.isAnnotationPresentByType(
                                                field.getElement(),
                                                ShardEntity.class
                                        ) ?
                                                "().getId())\n" :
                                                "())\n"
                                )
                )
                .reduce(
                        "    private void additionalPersist(" + entityClassDto.getTargetClassName() +
                                " entity) {\n" +
                                "        if (entity.hasNewShards()) {\n" +
                                "            entityManager\n" +
                                "                    .createQueries(entity, INS_QUERY, QueryType.DML," +
                                " QueryStrategy.NEW_SHARDS)\n" +
                                "                    .forEach(query ->\n" +
                                "                            query\n" +
                                "                                    .bind(entityManager.getTransactionUUID())\n" +
                                "                                    .bindShardMap(entity)\n",
                        String::concat
                )
                .concat(
                        """
                                                                    .bind(entity.getId())
                                                                    .addBatch()
                                                    );
                                        }
                                """
                )
                .concat(entityClassDto.getUniqueFields().isEmpty() ?
                        StringUtils.EMPTY :
                        entityClassDto.getUniqueFields()
                                .stream()
                                .map(field ->
                                        "                    .bind(entity." +
                                                field.getGetter() +
                                                (
                                                        ProcessorUtils.isAnnotationPresentByType(
                                                                field.getElement(),
                                                                ShardEntity.class
                                                        ) ?
                                                                "().getId()" :
                                                                "()"
                                                ) + ", isUpdate && !entity.isChanged(" + field.getColumnIndex() + "))\n"
                                )
                                .reduce(
                                        """
                                                        boolean isUpdate = entity.isStored();
                                                        if (!entity.hasMainShard() && (!isUpdate || entity.isChanged\
                                                () && (entity.getChanges() & UNIQUE_COLUMNS) > 0L)) {
                                                            entityManager
                                                                    .createQuery(
                                                                            entity,
                                                                            isUpdate ?
                                                                                    getUpdateSQL(entity.getChanges()\
                                                 & UNIQUE_COLUMNS) :
                                                                                    INS_UNIQUE_FIELDS_QUERY,
                                                                            QueryType.DML,
                                                                            QueryStrategy.MAIN_SHARD
                                                                    )
                                                                    .bind(entityManager.getTransactionUUID())
                                                                    .bindShardMap(entity)
                                                """,
                                        String::concat
                                )
                                .concat(
                                        """
                                                                    .bind(entity.getId())
                                                                    .addBatch();
                                                        }
                                                """
                                )
                )
                .concat("    }");
    }

    private static String getClusterCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public Cluster getCluster() {\n" +
                "        return cluster;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Cluster getCluster(" + entityClassDto.getTargetClassName() + " entity) {\n" +
                "        return Optional.ofNullable(entity).map(ShardInstance::getCluster).orElse(cluster);\n" +
                "    }";
    }

    private static String getSetEntityManagerCode() {
        return """
                    @Override
                    public void setEntityManager(ShardEntityManager entityManager) {
                        this.entityManager = entityManager;
                    }\
                """;
    }

    private static String getShardTypeCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public ShardType getShardType() {\n" +
                "        return SHARD_TYPE;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public ShardType getShardType(" + entityClassDto.getTargetClassName() + " entity) {\n" +
                "        return SHARD_TYPE;\n" +
                "    }";
    }

    private static String getSetDependentStorageCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field ->
                        ProcessorUtils.isAnnotationPresent(field.getElement(), ParentShard.class) ||
                                ProcessorUtils.isAnnotationPresentByType(field.getElement(), ShardEntity.class) ||
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), ShardEntity.class) ?
                            "        entityManager." +
                                    (ProcessorUtils.isAnnotationPresentInArgument(
                                            field.getElement(),
                                            ShardEntity.class
                                    ) ?
                                            "setAllStorage" :
                                            "setStorage"
                                    ) + "(entity." + field.getGetter() + "(), " +
                                    (
                                            ProcessorUtils.isAnnotationPresent(field.getElement(), ParentShard.class) &&
                                                    entityClassDto.getShardType() != ShardType.REPLICABLE ?
                                                    "entity" :
                                                    "null"
                                    ) +
                                    ");\n" :
                            ""
                )
                .reduce(
                        "    @Override\n" +
                                "    public void setDependentStorage(" + entityClassDto.getTargetClassName() +
                                " entity) {\n",
                        String::concat
                ) + "    }";
    }


    private static String getGenerateDependentIdCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field -> {
                    String code = "";
                    if (ProcessorUtils.isAnnotationPresentByType(field.getElement(), ShardEntity.class)) {
                        code = "        entityManager.generateId(entity." + field.getGetter() + "());\n";
                    }
                    if (ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), ShardEntity.class)) {
                        code = "        entityManager.generateAllId(entity." + field.getGetter() + "());\n";
                        if (field.getIsLinked()) {
                            EntityFieldDto linkedField = findFieldByLinkedColumn(field);
                            if (Objects.nonNull(linkedField)) {
                                code = code + "        entity." + field.getGetter() + "()\n" +
                                        "                .stream()\n" +
                                        "                .filter(child -> \n" +
                                        "                        Optional.ofNullable(child." +
                                        linkedField.getGetter() + "())\n" +
                                        (
                                                ProcessorUtils.isAnnotationPresentByType(
                                                        linkedField.getElement(), ShardEntity.class
                                                ) ?
                                                        "                                .map(it -> it.getId())\n" :
                                                        StringUtils.EMPTY
                                        ) +
                                        "                                .map(it -> !it.equals(entity.getId()))\n" +
                                        "                                .orElse(true)\n" +
                                        "                )\n" +
                                        "                .forEach(it -> it." + linkedField.getSetter() + "(entity" +
                                        (
                                                ProcessorUtils.isAnnotationPresentByType(
                                                        linkedField.getElement(), ShardEntity.class
                                                ) ?
                                                        StringUtils.EMPTY :
                                                        ".getId()"
                                        ) + "));\n";
                            }
                        }
                    }
                    return code;
                })
                .reduce(
                        "    @Override\n" +
                                "    public void generateDependentId(" + entityClassDto.getTargetClassName() +
                                " entity) {\n",
                        String::concat
                ) + "    }";
    }

    private static String getLockCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public void lock(" + entityClassDto.getTargetClassName() + " entity) {\n" +
                "        entityManager\n" +
                "                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)\n" +
                "                .bind(entity.getId())\n" +
                "                .execute();\n" +
                "    }\n";
    }
}
