package com.wanda.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String username;

    private String email;

    private String password;

    private Boolean isPremiumUser = false;

    private Boolean enabled = true;
}