package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.Storage;
import com.antalex.db.dao.entity.ClientEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.db.model.enums.MappingType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.FetchType;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(
        value = ClientEntity.class,
        storage = @Storage(fetchType = FetchType.LAZY)
)
public class ClientDomain extends BaseDomain {
    @Attribute
    private String name;
    @Attribute(mappingType = MappingType.STORAGE)
    private String fullName;
    @Attribute(mappingType = MappingType.STORAGE)
    private LocalDateTime createDate;
}
