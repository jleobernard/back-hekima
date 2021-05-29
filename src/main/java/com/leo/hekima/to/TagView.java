package com.leo.hekima.to;

public class TagView {
    private String uri;
    private String valeur;
    private String valeurRecherche;
    private TagView is;

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

    public TagView(String uri, String valeur, String valeurRecherche, TagView is) {

        this.uri = uri;
        this.valeur = valeur;
        this.valeurRecherche = valeurRecherche;
        this.is = is;
    }
}
