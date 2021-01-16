package com.leo.hekima.to;

public class TagView {
    private String uri;
    private String valeur;
    private String valeurRecherche;
    private TagView is;
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

    public TagView getIs() {
        return is;
    }

    public void setIs(TagView is) {
        this.is = is;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public TagView(String uri, String valeur, String valeurRecherche, TagView is, long lastUsed) {

        this.uri = uri;
        this.valeur = valeur;
        this.valeurRecherche = valeurRecherche;
        this.is = is;
        this.lastUsed = lastUsed;
    }
}
