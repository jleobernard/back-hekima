package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("refresh_token")
public class RefreshTokenModel {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("token")
    private String token;

    public RefreshTokenModel(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
