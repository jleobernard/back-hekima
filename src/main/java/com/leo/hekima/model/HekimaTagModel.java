package com.leo.hekima.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Tag")
public class HekimaTagModel {
    @Id
    private String uri;
    private String valeur;
    private String valeurRecherche;
    @Relationship(type = "IS", direction = Relationship.Direction.OUTGOING)
    private HekimaTagModel is;
    private long lastUsed;

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

    public HekimaTagModel getIs() {
        return is;
    }

    public void setIs(HekimaTagModel is) {
        this.is = is;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
