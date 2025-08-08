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
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Table(name = "T_ACCOUNT",
       indexes = {
               @Index(columnList = "code"),
               @Index(columnList = "dateOpen")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class AccountEntity extends BaseShardEntity {
    private String code;
    @ParentShard
    @OneToOne
    @JoinColumn
    private ClientEntity client;
    private BigDecimal balance;
    private OffsetDateTime dateOpen;
    @OneToMany
    @JoinColumn(name = "C_ACCOUNT")
    private List<AccountStatementEntity> statements = new ArrayList<>();
}
