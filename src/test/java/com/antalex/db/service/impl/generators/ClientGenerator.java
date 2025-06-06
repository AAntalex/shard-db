package com.antalex.db.service.impl.generators;

import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.dao.domain.Contract;
import com.antalex.db.service.DataGeneratorService;
import com.antalex.db.service.DomainEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class ClientGenerator implements DataGeneratorService<ClientDomain> {
    @Autowired
    private DomainEntityManager domainManager;
    @Autowired
    private ClientCategoryGenerator clientCategoryGenerator;

    @Override
    public List<ClientDomain> generate(int count) {
        List<ClientCategoryDomain> categories = clientCategoryGenerator.generate(1);
        List<ClientDomain> clients = domainManager.findAll(ClientDomain.class);
        clients.addAll(
                IntStream.rangeClosed(clients.size() + 1, count)
                        .mapToObj(idx ->
                                domainManager.newDomain(ClientDomain.class)
                                        .name("CLIENT" + idx)
                                        .category(idx % 100 == 1 ? categories.get(0) : null)
                                        .createDate(LocalDateTime.now())
                                        .fullName("FULL NAME" + idx)
                                        .contract(
                                                new Contract()
                                                        .description("CONTRACT" + idx)
                                                        .date(LocalDate.now())
                                        )
                        )
                        .toList()
        );
        domainManager.updateAll(clients);
        return clients;
    }
}
