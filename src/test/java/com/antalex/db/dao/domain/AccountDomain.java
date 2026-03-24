package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.Storage;
import com.antalex.db.dao.entity.AccountEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.FetchType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(
        value = AccountEntity.class,
        storage = @Storage(fetchType = FetchType.LAZY)
)
public class AccountDomain extends BaseDomain {
    @Attribute
    private String code;
    @Attribute
    private ClientDomain client;
    @Attribute
    private BigDecimal balance;
    @Attribute
    private OffsetDateTime dateOpen;
}
