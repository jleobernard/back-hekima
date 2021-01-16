package com.leo.hekima.to;

public class SourceView {
    private final String uri;
    private final String titre;
    private final String titreRecherche;
    private final String auteur;
    private final String type;
    private final long lastUsed;

    public SourceView(String uri, String titre, String titreRecherche, String auteur, String type, long lastUsed) {
        this.uri = uri;
        this.titre = titre;
        this.titreRecherche = titreRecherche;
        this.auteur = auteur;
        this.type = type;
        this.lastUsed = lastUsed;
    }

    public String getUri() {
        return uri;
    }

    public String getTitre() {
        return titre;
    }

    public String getTitreRecherche() {
        return titreRecherche;
    }

    public String getAuteur() {
        return auteur;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public String getType() {
        return type;
    }
}
