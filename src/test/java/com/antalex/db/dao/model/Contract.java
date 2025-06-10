package com.antalex.db.dao.model;

import com.antalex.db.dao.model.enums.ContractType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class Contract {
    private String description;
    private LocalDate date;
    private ContractType contractType;
    private List<String> additions;
}
