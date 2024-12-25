package com.wanda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisUserDTO implements Serializable {
    private String username;
    private String email;
    private Boolean isPremium = false;
}
