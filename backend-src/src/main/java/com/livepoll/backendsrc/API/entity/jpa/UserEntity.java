package com.livepoll.backendsrc.API.entity.jpa;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    //Constructors
    public UserEntity(){}

    //Columns
    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;



    // Getters / Setters
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword(){return password;}
}
