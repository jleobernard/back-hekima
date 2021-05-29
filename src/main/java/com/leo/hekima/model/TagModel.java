package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("tag")
public class TagModel {
    @Id
    private Long id;
    private String uri;
    private String valeur;
    @Column("valeur_recherche")
    private String valeurRecherche;
    @Column("last_used")
    private Instant lastUsed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public String getValeurRecherche() {
        return valeurRecherche;
    }

    public void setValeurRecherche(String valeurRecherche) {
        this.valeurRecherche = valeurRecherche;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }
}
