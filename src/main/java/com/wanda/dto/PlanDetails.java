package com.wanda.dto;

import lombok.Data;

@Data
public class PlanDetails {

    private long amount;

    private long quantity;

    private String name;

    private String currency;

    private String email;
}
