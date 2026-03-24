package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.dao.entity.ClientCategoryEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.db.model.enums.MappingType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(ClientCategoryEntity.class)
public class ClientCategoryDomain extends BaseDomain {
    @Attribute(name = "code")
    private String categoryCode;
    @Attribute(mappingType = MappingType.STORAGE)
    private String description;
}
