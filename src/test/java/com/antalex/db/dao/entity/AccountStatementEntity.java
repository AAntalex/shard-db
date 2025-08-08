package com.antalex.db.dao.entity;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Table(name = "T_ACCOUNT_STATEMENT",
        indexes = {
               @Index(name = "IDX_ACCOUNT_STATEMENT_ACCOUNT_DATE", columnList = "account,date"),
               @Index(columnList = "payment")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class AccountStatementEntity extends BaseShardEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private AccountEntity account;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private PaymentEntity payment;
    private BigDecimal sum;
    private OffsetDateTime date;
}
