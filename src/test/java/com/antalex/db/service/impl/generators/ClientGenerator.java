package com.antalex.db.service.impl.generators;

import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.service.DataGeneratorService;
import com.antalex.db.service.DomainEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Component
public class ClientGenerator implements DataGeneratorService<ClientDomain> {
    @Autowired
    private DomainEntityManager entityManager;
    @Autowired
    private ClientCategoryGenerator clientCategoryGenerator;

    @Override
    public List<ClientDomain> generate(int count) {
        List<ClientCategoryDomain> categories = clientCategoryGenerator.generate(1);
        List<ClientDomain> clients = entityManager.findAll(ClientDomain.class);
        clients.addAll(
                IntStream.rangeClosed(clients.size() + 1, count)
                        .mapToObj(idx ->
                                entityManager.newDomain(ClientDomain.class)
                                        .name("CLIENT" + idx)
                                        .category(idx % 100 == 1 ? categories.get(0) : null)
                        )
                        .toList()
        );
        entityManager.updateAll(clients);
        return clients;
    }
}
