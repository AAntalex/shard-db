package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.*;
import com.antalex.db.model.dto.*;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import java.util.*;

public class CriteriaClassBuilder {
    private static final Map<Element, CriteriaDto> criteriaClasses = new HashMap<>();

    public static CriteriaDto getCriteriaDtoByElement(Element classElement) {
        Criteria criteria = classElement.getAnnotation(Criteria.class);
        if (criteria == null) {
            return null;
        }
        if (!criteriaClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            EntityClassDto entityClass = Optional.ofNullable(getElement(criteria))
                    .map(EntityClassBuilder::getClassDtoByElement)
                    .orElse(null);

            CriteriaDto criteriaDto = CriteriaDto
                    .builder()
                    .targetClassName(elementName)
                    .classPackage(ProcessorUtils.getPackage(classElement.asType().toString()))
                    .from(entityClass)
                    .alias(criteria.alias())
                    .where(criteria.where())
                    .joins(
                            Arrays.stream(criteria.joins())
                                    .map(join ->
                                            CriteriaJoinDto
                                                    .builder()
                                                    .from(
                                                            Optional.ofNullable(getElement(join))
                                                                    .map(EntityClassBuilder::getClassDtoByElement)
                                                                    .orElse(null)
                                                    )
                                                    .alias(join.alias())
                                                    .on(join.on())
                                                    .joinType(join.joinType())
                                                    .build()
                                    )
                                    .toList()
                    )
                    .build();
            criteriaClasses.put(classElement, criteriaDto);
        }
        return criteriaClasses.get(classElement);
    }

    private static Element getElement(Criteria criteria) {
        try {
            criteria.from();
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
        return null;
    }

    private static Element getElement(Join join) {
        try {
            join.from();
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
        return null;
    }
}