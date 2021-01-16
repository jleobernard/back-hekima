package com.leo.hekima.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Source")
public class HekimaSourceModel {
    @Id
    private String uri;
    private String titre;
    private String titreRecherche;
    private String auteur;
    private String type;
    private long lastUsed;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getTitreRecherche() {
        return titreRecherche;
    }

    public void setTitreRecherche(String titreRecherche) {
        this.titreRecherche = titreRecherche;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
