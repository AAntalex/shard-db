package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.dto.*;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.MappingType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.api.DataWrapper;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.antalex.db.annotation.Storage;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.persistence.FetchType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class DomainClassBuilder {
    private static final Map<Element, DomainClassDto> domainClasses = new HashMap<>();

    public static DomainClassDto getClassDtoByElement(Element classElement) {
        DomainEntity domainEntity = classElement.getAnnotation(DomainEntity.class);
        if (domainEntity == null) {
            return null;
        }
        if (!domainClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            EntityClassDto entityClass = Optional.ofNullable(getElement(domainEntity))
                    .map(EntityClassBuilder::getClassDtoByElement)
                    .orElse(null);

            boolean isFluent = Optional.ofNullable(classElement.getAnnotation(Accessors.class))
                    .map(Accessors::fluent)
                    .orElse(false);
            Map<String, String> getters = ProcessorUtils.getMethodsByPrefix(classElement, "get");
            Map<String, String> setters = ProcessorUtils.getMethodsByPrefix(classElement, "set");

            StorageDto mainStorage = getMainStorage(elementName, domainEntity);
            Map<String, StorageDto> storageDtoMap = getStorageMap(mainStorage, domainEntity);

            DomainClassDto domainClassDto = DomainClassDto
                    .builder()
                    .targetClassName(elementName)
                    .classPackage(ProcessorUtils.getPackage(classElement.asType().toString()))
                    .entityClass(entityClass)
                    .storage(mainStorage)
                    .cluster(domainEntity.cluster())
                    .classElement(classElement)
                    .storageMap(storageDtoMap)
                    .chainAccessors(
                            Optional.ofNullable(classElement.getAnnotation(Accessors.class))
                                    .map(Accessors::chain)
                                    .orElse(false)
                    )
                    .fields(
                            ElementFilter.fieldsIn(classElement.getEnclosedElements())
                                    .stream()
                                    .map(
                                            fieldElement ->
                                                    DomainFieldDto
                                                            .builder()
                                                            .fieldName(fieldElement.getSimpleName().toString())
                                                            .getter(
                                                                    ProcessorUtils.
                                                                            findGetter(getters, fieldElement, isFluent)
                                                            )
                                                            .setter(
                                                                    ProcessorUtils.
                                                                            findSetter(setters, fieldElement, isFluent)
                                                            )
                                                            .element(fieldElement)
                                                            .entityField(getEntityField(fieldElement, entityClass))
                                                            .storage(
                                                                    getStorage(fieldElement, mainStorage, storageDtoMap)
                                                            )
                                                            .build()
                                    )
                                    .collect(Collectors.toList())
                    )
                    .build();

            int idx = 0;
            for (DomainFieldDto fieldDto : domainClassDto.getFields()) {
                if (Objects.nonNull(fieldDto.getEntityField())) {
                    fieldDto.setFieldIndex(++idx);
                }
            }

            domainClasses.put(classElement, domainClassDto);
        }
        return domainClasses.get(classElement);
    }

    private static StorageDto getStorageDto(Storage storage) {
        return StorageDto.builder()
                .name(storage.value())
                .dataFormat(storage.dataFormat())
                .cluster(storage.cluster())
                .shardType(storage.shardType())
                .fetchType(storage.fetchType())
                .isUsed(false)
                .build();
    }

    private static StorageDto getMainStorage(String elementName, DomainEntity domainEntity) {
        StorageDto storageDto = getStorageDto(domainEntity.storage());
        if ("<DEFAULT>".equals(storageDto.getName())) {
            storageDto.setName(elementName);
        }
        return storageDto;
    }

    private static Map<String, StorageDto> getStorageMap(StorageDto mainStorage, DomainEntity domainEntity) {
        Map<String, StorageDto> storageDtoMap = new HashMap<>();
        storageDtoMap.put(mainStorage.getName(), mainStorage);
        for (Storage storage : domainEntity.additionalStorage()) {
            storageDtoMap.put(storage.value(), getStorageDto(storage));
        }
        return storageDtoMap;
    }

    private static EntityFieldDto getEntityField(Element element, EntityClassDto entityClass) {
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

    private static StorageDto getStorage(
            Element element,
            StorageDto mainStorage,
            Map<String, StorageDto> storageDtoMap)
    {
        return Optional.ofNullable(element.getAnnotation(Attribute.class))
                .filter(a -> a.mappingType() == MappingType.STORAGE)
                .map(a -> {
                    StorageDto storage = a.storage().isEmpty() ? mainStorage : storageDtoMap.get(a.storage());
                    if (Objects.isNull(storage)) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Хнинилище с именем %s отсутсвует в описани @DomainEntity(additionalStorage " +
                                                "= ...",
                                        a.storage()
                                )
                        );
                    }
                    storage.setIsUsed(true);
                    return storage;
                })
                .orElse(null);
    }

    private static Element getElement(DomainEntity domainEntity) {
        try {
            domainEntity.value();
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
        return null;
    }

    public static void createInterceptorClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        DomainClassDto domainClassDto = getClassDtoByElement(annotatedElement);
        if (domainClassDto == null) {
            return;
        }
        String className = domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + domainClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    getImportedTypes(
                            domainClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            ShardEntityManager.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName(),
                                            DomainEntityManager.class.getCanonicalName(),
                                            DataStorage.class.getCanonicalName(),
                                            Map.class.getCanonicalName(),
                                            AttributeStorage.class.getCanonicalName(),
                                            DataWrapper.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println(
                    "public class " + className + " extends " + domainClassDto.getTargetClassName() + " {\n" +
                            "    private DomainEntityManager domainManager;\n" +
                            getLazyFlagsCode(domainClassDto) +
                            "\n    public " + className + "(" + domainClassDto.getEntityClass().getTargetClassName() +
                            " entity, DomainEntityManager domainManager) {\n" +
                            "        this.entity = entity;\n" +
                            "        this.domainManager = domainManager;\n" +
                            "    }\n"
            );
            out.println(getSetLazyCode(domainClassDto));
            out.println(getGettersCode(domainClassDto));
            out.println(getSettersCode(domainClassDto));
            out.println(getReadEntityCode(domainClassDto));
            out.println(getReadFromStorageCode(domainClassDto));
            out.println("}");
        }
    }

    public static void createMapperClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        DomainClassDto domainClassDto = getClassDtoByElement(annotatedElement);
        if (domainClassDto == null) {
            return;
        }
        String className = domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_MAPPER_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + domainClassDto.getClassPackage() + ";");
            out.println();

            out.println(
                    "import " + domainClassDto.getClassPackage() + "." +
                            domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX + ";"
            );
            out.println(
                    getImportedTypes(
                            domainClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            "java.util.*",
                                            ShardInstance.class.getCanonicalName(),
                                            Domain.class.getCanonicalName(),
                                            Autowired.class.getCanonicalName(),
                                            DomainEntityMapper.class.getCanonicalName(),
                                            DomainEntityManager.class.getCanonicalName(),
                                            Component.class.getCanonicalName(),
                                            AttributeStorage.class.getCanonicalName(),
                                            DataStorage.class.getCanonicalName(),
                                            DataFormat.class.getCanonicalName(),
                                            ShardType.class.getCanonicalName(),
                                            ShardDataBaseManager.class.getCanonicalName(),
                                            DataWrapper.class.getCanonicalName(),
                                            FetchType.class.getCanonicalName(),
                                            Cluster.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println(
                    "@Component\n" +
                    "public class " + className + " implements DomainEntityMapper<" +
                            domainClassDto.getTargetClassName() + ", " +
                            domainClassDto.getEntityClass().getTargetClassName() + "> {\n" +
                            "    private DomainEntityManager domainManager;\n\n" +
                            "    private ThreadLocal<Map<Long, Domain>> domains = " +
                            "ThreadLocal.withInitial(HashMap::new);\n" +
                            "    private final Map<String, DataStorage> storageMap = new HashMap<>();\n" +
                            "    private final Cluster cluster;\n\n" +
                            getConstructorMapperCode(domainClassDto, className) +
                            "\n\n" +
                            "    @Override\n" +
                            "    public void setDomainManager(DomainEntityManager domainManager) {\n" +
                            "        this.domainManager = domainManager;\n" +
                            "    }\n" +
                            "\n" +
                            "    @Override\n" +
                            "    public Map<String, DataStorage> getDataStorage() {\n" +
                            "        return storageMap;\n" +
                            "    }\n" +
                            "\n" +
                            "    @Override\n" +
                            "    public " + domainClassDto.getTargetClassName() + " newDomain(" +
                            domainClassDto.getEntityClass().getTargetClassName() + " entity) {\n" +
                            "        entity.setCluster(this.cluster);\n" +
                            "        return new " + domainClassDto.getTargetClassName() +
                            ProcessorUtils.CLASS_INTERCEPT_POSTFIX + "(entity, domainManager);\n" +
                            "    }\n"
            );
            out.println(getMapToEntityCode(domainClassDto));
            out.println();
            out.println(getMapToDomainCode(domainClassDto));
            out.println();
            out.println(getMapStorageToEntityCode(domainClassDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(DomainClassDto domainClassDto, List<String> importedTypes) {
        domainClassDto.getFields()
                .stream()
                .map(DomainFieldDto::getElement)
                .filter(element -> ProcessorUtils.isAnnotationPresent(element, Attribute.class))
                .map(ProcessorUtils::getDeclaredType)
                .filter(Objects::nonNull)
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
        importedTypes.add(domainClassDto.getEntityClass().getClassElement().asType().toString());
        return ProcessorUtils.getImportedTypes(importedTypes);
    }

    private static String getGettersCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        ProcessorUtils.isAnnotationPresent(field.getElement(), Attribute.class) &&
                                (
                                        Objects.nonNull(field.getEntityField()) ||
                                                Objects.nonNull(field.getStorage())
                                ) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                        "\n    @Override\n" +
                                "    public " + ProcessorUtils.getTypeField(field.getElement()) + " " +
                                field.getGetter() + "() {\n" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?
                                                (
                                                        ProcessorUtils.isAnnotationPresentInArgument(
                                                                field.getElement(),
                                                                DomainEntity.class
                                                        ) ?
                                                                "        if (" + field.getFieldName() + "Lazy) {\n" +
                                                                        "            this." + field.getSetter() +
                                                                        "(domainManager.mapAllToDomains(" +
                                                                        ProcessorUtils.getFinalType(
                                                                                field.getElement()
                                                                        ) +
                                                                        ".class, ((" +
                                                                        classDto.getEntityClass().getTargetClassName() +
                                                                        ") entity)." +
                                                                        field.getEntityField().getGetter() + "()));\n" +
                                                                        "        }\n" :
                                                                """
                                                                                if (isLazy) {
                                                                                    readEntity();
                                                                                }
                                                                        """
                                                ) :
                                                "        if (this.isLazy(\"" + field.getStorage().getName() +
                                                        "\")) {\n" +
                                                        "            readFromStorage(\"" +
                                                        field.getStorage().getName() + "\");\n" +
                                                        "        }\n" +
                                                        (
                                                                ProcessorUtils.hasFinalType(field.getElement()) ?
                                                                        StringUtils.EMPTY :
                                                                        "        this.setChanges(\"" +
                                                                                field.getStorage().getName() +
                                                                                "\");\n"
                                                        )


                                ) +
                                "        return super." + field.getGetter() + "();\n" +
                                "    }\n"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it ->
                        ProcessorUtils.isAnnotationPresent(it.getElement(), Attribute.class) &&
                                Objects.nonNull(it.getSetter()) &&
                                !ProcessorUtils.isAnnotationPresentInArgument(it.getElement(), DomainEntity.class)
                )
                .map(field ->
                        "\n    public " + (classDto.getChainAccessors() ? classDto.getTargetClassName() : "void") +
                                " " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) +
                                " value, boolean change) {\n" +
                                "        if (change) {\n" +
                                "            if (this.isLazy(" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?
                                                StringUtils.EMPTY :
                                                "\"" + field.getStorage().getName() + "\""
                                ) + ")) {\n" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?
                                                "                readEntity();\n" :
                                                "                readFromStorage(\"" + field.getStorage().getName() +
                                                        "\");\n"
                                ) +
                                "            }\n" +
                                "            this.setChanges(" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?
                                                field.getFieldIndex() :
                                                "\"" + field.getStorage().getName() + "\""
                                ) + ");\n" +
                                "        }\n" +
                                "        " + (classDto.getChainAccessors() ? "return " : StringUtils.EMPTY) +
                                "super." + field.getSetter() + "(value);\n" +
                                "    }\n\n" +
                                "    @Override\n" +
                                "    public " +
                                (classDto.getChainAccessors() ? classDto.getTargetClassName() : "void") +
                                " " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) + " value) {\n" +
                                "        " + (classDto.getChainAccessors() ? "return " : StringUtils.EMPTY) +
                                field.getSetter() + "(value, true);\n" +
                                "    }\n"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getReadEntityCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                Objects.nonNull(field.getEntityField().getGetter()) &&
                                Objects.nonNull(field.getSetter()) &&
                                !ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field ->
                                        "\n        this." + field.getSetter() + "(" +
                                        (
                                                ProcessorUtils.isAnnotationPresentByType(
                                                        field.getEntityField().getElement(),
                                                        ShardEntity.class
                                                ) ?

                                                        "domainManager.map(" +
                                                                ProcessorUtils.getTypeField(field.getElement()) +
                                                                ".class, entity." +
                                                                field.getEntityField().getGetter() + "()" :
                                                        "entity." + field.getEntityField().getGetter() + "("
                                        ) +
                                        "), false);"
                )
                .reduce(
                        "    private void readEntity() {\n" +
                                "        " + classDto.getEntityClass().getTargetClassName() + " entity = (" +
                                classDto.getEntityClass().getTargetClassName() + ") this.entity;",
                        String::concat
                ) +
                "\n        this.isLazy = false;" +
                "\n    }\n";
    }

    private static String getMapToDomainCode(DomainClassDto classDto) {
        return
                "    @Override\n" +
                        "    public " + classDto.getTargetClassName() + " map(" +
                        classDto.getEntityClass().getTargetClassName() + " entity) {\n" +
                        "        if (!Optional.ofNullable(entity).map(ShardInstance::getId).isPresent()) {\n" +
                        "            return null;\n" +
                        "        }\n" +
                        "        " + classDto.getTargetClassName() + " domain = (" + classDto.getTargetClassName() +
                        ") domains.get().get(entity.getId());\n" +
                        "        if (Objects.isNull(domain)) {\n" +
                        "            domain = newDomain(entity);\n" +
                        "            domains.get().put(entity.getId(), domain);\n" +
                        "        }\n" +
                        "        for (String storageName : storageMap.keySet()) {\n" +
                        "            domain.setLazy(storageName, true);\n" +
                        "        }\n" +
                        "        Map<String, AttributeStorage> storage = domain.getStorage();\n" +
                        "        entity.getAttributeStorage()\n" +
                        "                .forEach(attributeStorage -> storage.put(attributeStorage.getStorageName()," +
                        " attributeStorage));\n" +
                        "        domain.setLazy(true);\n" +
                        "        entity.setHasDomain(true);\n" +
                        "        return domain;\n" +
                        "    }";
    }

    private static String getMapToEntityCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                Objects.nonNull(field.getEntityField().getSetter()) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class) ?
                                        "\n        entity." + field.getEntityField().getSetter() +
                                                "(domainManager.mapAllToEntities(" +
                                                ProcessorUtils.getFinalType(field.getElement()) +
                                                ".class, domain." +  field.getGetter() + "()));" :
                                        "\n        if (domain.isChanged(" + field.getFieldIndex() + ")) {\n" +
                                                "            entity." + field.getEntityField().getSetter() +
                                                (
                                                        ProcessorUtils.isAnnotationPresentByType(
                                                                field.getElement(),
                                                                DomainEntity.class
                                                        ) ?
                                                                "(domainManager.map(" +
                                                                        ProcessorUtils.getFinalType(
                                                                                field.getElement()
                                                                        ) +
                                                                        ".class, domain." + field.getGetter() +
                                                                        "()));\n" :
                                                                "(domain." + field.getGetter() + "());\n"
                                                ) + "        }"
                )
                .reduce(
                        "    @Override\n" +
                                "    public " + classDto.getEntityClass().getTargetClassName() +
                                " map(" + classDto.getTargetClassName() + " domain) {\n" +
                                "        " + classDto.getEntityClass().getTargetClassName() +
                                " entity = domain.getEntity();",
                        String::concat
                ) +
                "\n        domain.dropChanges();\n" +
                "        entity.setAttributeStorage(mapStorage(domain));\n" +
                "        entity.setHasDomain(true);\n" +
                "        return entity;\n" +
                "    }";
    }

    private static String getMapStorageToEntityCode(DomainClassDto classDto) {
        return classDto.getStorageMap()
                .keySet()
                .stream()
                .map(storageDto -> getMapStorageCode(classDto, storageDto)
                )
                .reduce(
                        "    private List<AttributeStorage> mapStorage(" + classDto.getTargetClassName() +
                                " domain) {\n" +
                                "        List<AttributeStorage> storage = new ArrayList<>();\n" +
                                "        try {",
                        String::concat
                ) +
                "\n        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return storage;\n" +
                "    }";
    }

    private static String getMapStorageCode(DomainClassDto classDto, String storageName) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getStorage()) &&
                                storageName.equals(field.getStorage().getName())
                )
                .map(field ->
                                "\n                dataWrapper.put(\"" + field.getFieldName() + "\", domain." +
                                field.getGetter() + "());"
                )
                .reduce(
                        "\n            if (domain.isChanged(\"" + storageName + "\")) {\n" +
                                "                AttributeStorage attributeStorage = " +
                                "domainManager.getAttributeStorage(domain, storageMap.get(\"" +
                                storageName + "\"));\n" +
                                "                DataWrapper dataWrapper  = attributeStorage.getDataWrapper();",
                        String::concat
                ) +
                "\n                attributeStorage.setData(dataWrapper.getContent());\n" +
                "                domain.dropChanges(\"" + storageName + "\");\n" +
                "                storage.add(attributeStorage);\n" +
                "            }";
    }

    private static String getReadFromStorageCode(DomainClassDto classDto) {
        return classDto.getStorageMap()
                .keySet()
                .stream()
                .map(storageDto -> getReadStorageCode(classDto, storageDto)
                )
                .reduce(
                        "    private void readFromStorage(String storageName) {\n" +
                                "        Map<String, DataStorage> storageMap = domainManager.getDataStorage(" +
                                classDto.getTargetClassName() + ".class);\n" +
                                "        AttributeStorage attributeStorage = domainManager.getAttributeStorage(this, storageMap.get(storageName));\n" +
                                "        DataWrapper dataWrapper  = attributeStorage.getDataWrapper();\n" +
                                "        try {\n" +
                                "            switch (storageName) {",
                        String::concat
                ) +
                "\n            }\n" +
                "            setLazy(storageName, false);\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "    }";
    }

    private static String getReadStorageCode(DomainClassDto classDto, String storageName) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getStorage()) &&
                                storageName.equals(field.getStorage().getName())
                )
                .map(field ->
                                "\n                    " + field.getSetter() + "(dataWrapper." +
                                Optional.ofNullable(ProcessorUtils.getDeclaredType(field.getElement()))
                                        .map(DeclaredType::asElement)
                                        .map(Element::toString).map(ProcessorUtils::getClassByName)
                                        .map(clazz -> {
                                            if  (clazz.isAssignableFrom(Map.class)) {
                                                return
                                                        ProcessorUtils.getDeclaredType(field.getElement())
                                                                .getTypeArguments()
                                                                .stream()
                                                                .map(type -> (DeclaredType) type)
                                                                .map(DeclaredType::asElement)
                                                                .map(Element::getSimpleName)
                                                                .map(className -> " ," + className + ".class")
                                                                .reduce(
                                                                        "getMap(\"" +
                                                                                field.getFieldName() + "\"",
                                                                        String::concat
                                                                ) +
                                                                "), false);";
                                            }
                                            if  (clazz.isAssignableFrom(List.class)) {
                                                return "getList(\"" +
                                                        field.getFieldName() + "\", " +
                                                        ProcessorUtils.getFinalType(field.getElement()) +
                                                        ".class), false);";
                                            }
                                            return null;
                                        }).orElse(
                                                "get(\"" +
                                                        field.getFieldName() + "\", " + ProcessorUtils.getTypeField(field.getElement()) +
                                                        ".class), false);"
                                        )
                )
                .reduce(
                        "\n                case \"" + storageName + "\":",
                        String::concat
                ) +
                "\n                    break;";
    }


    private static String getLazyFlagsCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field -> "    private boolean " + field.getFieldName() + "Lazy = false;\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSetLazyCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field -> "        this." + field.getFieldName() + "Lazy = lazy;\n")
                .reduce(
                        """
                                    @Override
                                    public void setLazy(boolean lazy) {
                                """,
                        String::concat
                ) +
                "        super.setLazy(lazy);\n" +
                "    }";
    }


    private static String getConstructorMapperCode(DomainClassDto classDto, String className) {
        return classDto.getStorageMap()
                .values()
                .stream()
                .filter(StorageDto::getIsUsed)
                .map(storageDto ->
                        "\n        storageMap.put(\n" +
                                "                \"" + storageDto.getName() + "\",\n" +
                                "                DataStorage\n" +
                                "                        .builder()\n" +
                                "                        .name(\"" + storageDto.getName() + "\")\n" +
                                (
                                        storageDto.getCluster().isEmpty() ?
                                                StringUtils.EMPTY :
                                                "                        .cluster(dataBaseManager.getCluster(\"" +
                                                        storageDto.getCluster() + "\"))\n"
                                ) +
                                "                        .shardType(ShardType." +
                                storageDto.getShardType().name() + ")\n" +
                                "                        .dataFormat(DataFormat." +
                                storageDto.getDataFormat().name() + ")\n" +
                                "                        .fetchType(FetchType." +
                                storageDto.getFetchType().name() + ")\n" +
                                "                        .build());"
                )
                .reduce(
                        "    @Autowired\n" +
                                "    " + className + " (ShardDataBaseManager dataBaseManager) {",
                        String::concat) +
                "\n        this.cluster = " +
                (
                        classDto.getCluster().isEmpty() ?
                                "null;" :
                                "dataBaseManager.getCluster(String.valueOf(\"" + classDto.getCluster() + "\"));"
                )  +
                "\n    }";
    }
}
