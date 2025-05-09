package com.antalex.db.service.impl.generators;

import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.service.DataGeneratorService;
import com.antalex.db.service.DomainEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ClientCategoryGenerator implements DataGeneratorService<ClientCategoryDomain> {
    @Autowired
    private DomainEntityManager domainManager;

    @Override
    public List<ClientCategoryDomain> generate(int count) {
        return Collections.singletonList(
                Optional.ofNullable(domainManager.find(ClientCategoryDomain.class, "${categoryCode}=?", "VIP"))
                        .orElseGet(() -> domainManager.newDomain(ClientCategoryDomain.class)
                                .categoryCode("VIP")
                                .description("VIP-клиент")
                        )
        );
    }
}
