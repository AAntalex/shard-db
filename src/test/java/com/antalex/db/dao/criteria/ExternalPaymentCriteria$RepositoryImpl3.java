package com.antalex.db.dao.criteria;

import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.criteria.*;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class ExternalPaymentCriteria$RepositoryImpl3  {
    private static final List<String> COLUMNS = Arrays.asList(
            "MD.C_NUM",
            "MD.C_SUM",
            "MD.C_DATE",
            "ACC_DT.C_CODE",
            "ACC_CT.C_CODE",
            "CL_DT.C_NAME",
            "replace(cl_dt.c_name, ' ')",
            "CL_CT.C_NAME",
            "EXT_DOC.C_RECEIVER",
            "CL_CAT.ID"
    );

    private static final CriteriaElement ELEMENT_EXT_DOC = new CriteriaElement()
            .tableName("T_EXTERNAL_PAYMENT")
            .tableAlias("EXT_DOC")
            .shardType(ShardType.SHARDABLE)
            .index(0)
            .columns(256L);

    private static final CriteriaElement ELEMENT_MD = new CriteriaElement()
            .tableName("T_PAYMENT")
            .tableAlias("MD")
            .shardType(ShardType.MULTI_SHARDABLE)
            .index(1)
            .columns(7L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("MD.ID", "EXT_DOC.C_DOC"))
                            .element(ELEMENT_EXT_DOC)
            );

    private static final CriteriaElement ELEMENT_ACC_DT = new CriteriaElement()
            .tableName("T_ACCOUNT")
            .tableAlias("ACC_DT")
            .shardType(ShardType.SHARDABLE)
            .index(2)
            .columns(8L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("ACC_DT.ID", "MD.C_ACC_DT"))
                            .element(ELEMENT_MD)
            );

    private static final CriteriaElement ELEMENT_ACC_CT = new CriteriaElement()
            .tableName("T_ACCOUNT")
            .tableAlias("ACC_CT")
            .shardType(ShardType.SHARDABLE)
            .index(3)
            .columns(16L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.LEFT)
                            .linkedShard(true)
                            .joinColumns(Pair.of("ACC_CT.ID", "MD.C_ACC_CT"))
                            .element(ELEMENT_MD)
            );

    private static final CriteriaElement ELEMENT_CL_CT = new CriteriaElement()
            .tableName("T_CLIENT")
            .tableAlias("CL_CT")
            .shardType(ShardType.SHARDABLE)
            .index(4)
            .columns(128L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.LEFT)
                            .linkedShard(true)
                            .joinColumns(Pair.of("CL_CT.ID", "ACC_CT.C_CLIENT"))
                            .element(ELEMENT_ACC_CT)
            );

    private static final CriteriaElement ELEMENT_CL_DT = new CriteriaElement()
            .tableName("T_CLIENT")
            .tableAlias("CL_DT")
            .shardType(ShardType.SHARDABLE)
            .index(5)
            .columns(96L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("CL_DT.ID", "ACC_DT.C_CLIENT"))
                            .element(ELEMENT_ACC_DT)
            );

    private static final CriteriaElement ELEMENT_CL_CAT = new CriteriaElement()
            .tableName("T_CLIENT_CATEGORY")
            .tableAlias("CL_CAT")
            .shardType(ShardType.REPLICABLE)
            .index(6)
            .columns(512L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.LEFT)
                            .linkedShard(false)
                            .joinColumns(Pair.of("CL_CAT.ID", "CL_CT.C_CATEGORY"))
                            .element(ELEMENT_CL_CT)
            );

    private static final List<CriteriaElement> ELEMENTS = Arrays.asList(
            ELEMENT_EXT_DOC,
            ELEMENT_MD,
            ELEMENT_ACC_DT,
            ELEMENT_ACC_CT,
            ELEMENT_CL_CT,
            ELEMENT_CL_DT,
            ELEMENT_CL_CAT
    );

    private static final List<PredicateGroup> PREDICATE_GROUPS = Arrays.asList(
            new PredicateGroup(15L, 7L),
            new PredicateGroup(19L, 3L)
    );

    private static final List<CriteriaPredicate> PREDICATES = Arrays.asList(
            new CriteriaPredicate()
                    .value("EXT_DOC.C_DATE<{:1}")
                    .aliasMask(1L),
            new CriteriaPredicate()
                    .value("EXT_DOC.C_RECEIVER IS NULL")
                    .aliasMask(1L),
            new CriteriaPredicate()
                    .value("MD.C_DATE_PROV<{:2}")
                    .aliasMask(2L),
            new CriteriaPredicate()
                    .value("CL_CAT.C_CODE='VIP'")
                    .aliasMask(64L),
            new CriteriaPredicate()
                    .value("ACC_DT.C_CODE LIKE '40702810%3'")
                    .aliasMask(4L)
    );

    private final ShardDataBaseManager dataBaseManager;
    private final ShardEntityManager entityManager;

    @Getter
    private final Map<Long, CriteriaPart> criteriaPartMap;


    public ExternalPaymentCriteria$RepositoryImpl3(ShardEntityManager entityManager, ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
        this.entityManager = entityManager;

        ELEMENT_EXT_DOC.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_MD.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_ACC_DT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_ACC_CT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_CT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_DT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_CAT.cluster(dataBaseManager.getCluster("DEFAULT"));

        this.criteriaPartMap = getCriteriaParts(null);
    }

    @Data
    private static class RawCriteria {
        private Map<Long, CriteriaPart> criteriaParts = new HashMap<>();
        private List<CriteriaElement> elementList;
        private List<CriteriaPredicate> predicateList;
        private PredicateGroup predicateGroup;
        private long processedElements;
    }

    public Map<Long, CriteriaPart> getCriteriaParts(PredicateGroup predicateGroup) {
        RawCriteria rawCriteria = new RawCriteria();
        rawCriteria.setPredicateList(PREDICATES);




        if (predicateGroup == null) {
            rawCriteria.setElementList(ELEMENTS);
        } else {
            rawCriteria.setPredicateGroup(predicateGroup);
            Long innerJoinAliases =
                    IntStream.range(0, PREDICATES.size())
                            .filter(idx ->
                                    (predicateGroup.getPredicateMask() & 1L << idx) > 0L &&
                                            !PREDICATES.get(idx).value().endsWith(" IS NULL") ||
                                            (predicateGroup.getSignMask() & 1L << idx) > 0L
                            )
                            .mapToObj(idx -> PREDICATES.get(idx).aliasMask())
                            .reduce(0L, (a, b) -> a | b);

            Long outerJoinAliases =
                    IntStream.range(0, ELEMENTS.size())
                            .filter(idx -> ELEMENTS.get(idx).join().joinType() == JoinType.LEFT)
                            .mapToObj(idx -> 1L << idx)
                            .reduce(0L, (a, b) -> a | b);

            rawCriteria.setElementList(ELEMENTS);
            ELEMENTS
                    .stream()
                    .map(el ->
                            new CriteriaElement()
                                    .tableName(el.tableName())
                                    .tableAlias(el.tableAlias())
                    )
                    .toList()



        }
        processRawCriteria(rawCriteria, null);
        return rawCriteria.getCriteriaParts();
    }

    private static List<CriteriaElement> copyElementList(List<CriteriaElement> criteriaElements, Long changedJoinAliases) {
        return criteriaElements
                .stream()
                .map(el ->
                        new CriteriaElement()
                                .tableName(el.tableName())
                                .tableAlias(el.tableAlias())
                )
                .toList();
    }

    private static CriteriaElement toInnerJoinElement(CriteriaElement criteriaElement) {
        return Optional.of(criteriaElement)
                .filter(el ->
                        Optional
                                .ofNullable(el.join())
                                .map(join -> join.joinType() == JoinType.INNER)
                                .orElse(true)
                )
                .orElseGet(() ->
                        new CriteriaElement()
                                .tableName(criteriaElement.tableName())
                                .tableAlias(criteriaElement.tableAlias())
                                .cluster(criteriaElement.cluster())
                                .columns(criteriaElement.columns())
                                .shardType(criteriaElement.shardType())
                                .index(criteriaElement.index())
                                .join(
                                        Optional
                                                .ofNullable(criteriaElement.join())
                                                .map(join ->
                                                        new CriteriaElementJoin()
                                                                .element(toInnerJoinElement(join.element()))
                                                                .joinType(JoinType.INNER)
                                                                .joinColumns(join.joinColumns())
                                                                .linkedShard(join.linkedShard())
                                                )
                                                .orElse(null)
                                )
                );
    }

    private static void processRawCriteria(
            RawCriteria rawCriteria,
            CriteriaElement element) {
        Map<Integer, CriteriaPartRelation> relations = new HashMap<>();
        if (element != null) {
            getRelation(element, rawCriteria, relations);
        }
        rawCriteria.getElementList().forEach(el -> getRelation(el, rawCriteria, relations));
        relations
                .values()
                .stream()
                .map(CriteriaPartRelation::part)
                .forEach(part -> processCriteriaPart(rawCriteria, part));
    }

    private static boolean checkPredicateInPart(
            CriteriaPredicate predicate,
            Integer predicateIndex,
            CriteriaPart part,
            Long predicateMask)
    {
        return (predicateMask & 1L << predicateIndex) > 0L &&
                (predicate.aliasMask() == 0L || (predicate.aliasMask() & part.aliasMask()) > 0L);
    }

    private static void processCriteriaPart(RawCriteria rawCriteria, CriteriaPart part) {
        if (!rawCriteria.getCriteriaParts().containsKey(part.aliasMask())) {
            part.predicateGroup(
                    new PredicateGroup(
                            Optional.ofNullable(rawCriteria.getPredicateGroup())
                                    .map(PredicateGroup::getPredicateMask)
                                    .map(mask ->
                                            IntStream.range(0, rawCriteria.getPredicateList().size())
                                                    .filter(idx ->
                                                            checkPredicateInPart(rawCriteria.getPredicateList().get(idx),
                                                                    idx,
                                                                    part,
                                                                    mask
                                                            )
                                                    )
                                                    .mapToObj(idx -> 1L << idx)
                                                    .reduce(0L, (a, b) -> a | b)
                                    )
                                    .orElse(0L),
                            Optional.ofNullable(rawCriteria.getPredicateGroup())
                                    .map(PredicateGroup::getSignMask)
                                    .orElse(0L)
                    )
            );
            rawCriteria.getCriteriaParts().put(part.aliasMask(), part);
        }
    }

    private static @NotNull CriteriaPartRelation getRelation(
            CriteriaElement element,
            RawCriteria rawCriteria,
            Map<Integer, CriteriaPartRelation> relations)
    {
        return Optional
                .ofNullable(relations.get(element.index()))
                .orElseGet(() -> {
                    CriteriaPart criteriaPart = getCriteriaPart(element, rawCriteria, relations);
                    CriteriaPartRelation relation =
                            new CriteriaPartRelation()
                                    .part(criteriaPart)
                                    .linkedColumn(
                                            Optional
                                                    .ofNullable(element.join())
                                                    .filter(it ->
                                                            (criteriaPart.aliasMask() & 1L << it.element().index()) > 0)
                                                    .map(join ->
                                                            join.linkedShard()
                                                                    ? join.joinColumns().getLeft()
                                                                    : "NOT_LINKED"
                                                    )
                                                    .orElse(null)
                                    );
                    relations.put(element.index(), relation);
                    return relation;
                });
    }

    private static boolean needJoinToCriteriaPart(
            CriteriaElement element,
            CriteriaElementJoin join,
            CriteriaPartRelation relation) {
        return join.element().cluster() == element.cluster() &&
                (element.shardType() == ShardType.REPLICABLE ||
                        element.cluster().getShards().size() == 1 ||
                        join.linkedShard() &&
                                (
                                        join.element().shardType() == ShardType.SHARDABLE ||
                                                Optional
                                                        .ofNullable(relation.linkedColumn())
                                                        .map(column ->
                                                                column.equals(join.joinColumns().getRight()))
                                                        .orElse(true)
                                )
                );
    }

    private static boolean needProcessRawCriteria(
            CriteriaElement element,
            CriteriaElementJoin join,
            RawCriteria rawCriteria
    ) {
        return join.element().cluster() == element.cluster() &&
                join.linkedShard() &&
                (rawCriteria.getProcessedElements() & 1L << element.index()) == 0;
    }

    private static @NotNull CriteriaPart getCriteriaPart(
            CriteriaElement element,
            RawCriteria rawCriteria,
            Map<Integer, CriteriaPartRelation> relations)
    {
        return Optional
                .ofNullable(element.join())
                .filter(join ->
                        element.shardType() == ShardType.SHARDABLE || relations.containsKey(join.element().index()))
                .map(join -> {
                    CriteriaPartRelation parentRelation = getRelation(join.element(), rawCriteria, relations);
                    CriteriaPart criteriaPart = parentRelation.part();
                    if (needJoinToCriteriaPart(element, join, parentRelation)) {
                        if (parentRelation.linkedColumn() == null) {
                            parentRelation.linkedColumn(join.joinColumns().getRight());
                        }
                        return addToCriteriaPart(element, criteriaPart, rawCriteria);
                    }
                    if (needProcessRawCriteria(element, join, rawCriteria)) {
                        processRawCriteria(rawCriteria, element);
                    }
                    CriteriaPart childPart = createCriteriaPart(element, rawCriteria);
                    criteriaPart
                            .joinColumns()
                            .add(
                                    Pair.of(
                                            element.join().joinColumns().getRight(),
                                            element.join().joinColumns().getLeft()
                                    )
                            );
                    return childPart;
                })
                .orElseGet(() -> createCriteriaPart(element, rawCriteria));
    }

    private static CriteriaPart addToCriteriaPart(CriteriaElement element, CriteriaPart criteriaPart, RawCriteria rawCriteria) {
        rawCriteria.setProcessedElements(rawCriteria.getProcessedElements() | 1L << element.index());
        return criteriaPart
                .aliasMask(criteriaPart.aliasMask() | 1L << element.index())
                .columns(criteriaPart.columns() | element.columns())
                .from(
                        criteriaPart.from() +
                                getJoinText(element.join().joinType()) +
                                getTableName(element) + getOn(element.join())
                );
    }

    private static CriteriaPart createCriteriaPart(CriteriaElement element, RawCriteria rawCriteria) {
        rawCriteria.setProcessedElements(rawCriteria.getProcessedElements() | 1L << element.index());
        CriteriaPart criteriaPart = new CriteriaPart()
                .aliasMask(1L << element.index())
                .columns(element.columns())
                .from(getTableName(element))
                .cluster(element.cluster());
        if (element.join() != null) {
            criteriaPart.dependent(element.join().joinType() != JoinType.INNER);
            criteriaPart.joinColumns().add(element.join().joinColumns());
        }
        return criteriaPart;
    }

    private static String getTableName(CriteriaElement element) {
        return element.tableName() + " " + element.tableAlias();
    }

    private static String getOn(CriteriaElementJoin join) {
        return " ON " + join.joinColumns().getLeft() + "=" + join.joinColumns().getRight();
    }
    private static String getJoinText(JoinType joinType) {
        return switch (joinType) {
            case INNER -> " JOIN ";
            case LEFT -> " LEFT JOIN ";
            case RIGHT -> " RIGHT JOIN ";
        };
    }

}
