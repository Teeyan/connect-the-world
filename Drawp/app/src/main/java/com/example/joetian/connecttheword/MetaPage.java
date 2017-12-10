package com.example.joetian.connecttheword;

import java.util.Comparator;

public class MetaPage {
    protected String owner;
    protected String imgURL;
    protected int numUpvotes;
    protected int numDownvotes;

    public MetaPage(String owner, String url, int up, int down) {
        this.owner = owner;
        this.imgURL = url;
        this.numUpvotes = up;
        this.numDownvotes = down;
    }

    public String getOwner() {
        return this.owner;
    }

    public String getImgUrl() {
        return this.imgURL;
    }

    public int getNumUpvotes() {
        return this.numUpvotes;
    }

    public int getNumDownvotes() {
        return this.numDownvotes;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setImgUrl(String url) {
        this.imgURL = url;
    }

    public void setNumUpvotes(int up) {
        this.numUpvotes = up;
    }

    public void setNumDownvotes(int down) {
        this.numDownvotes = down;
    }

    static class MetaPageComparator implements Comparator<MetaPage>{
        public int compare(MetaPage mp1, MetaPage mp2) {
            if(mp1.getNumUpvotes() > mp2.getNumUpvotes()) {
                return -1;
            }
            else if(mp1.getNumUpvotes() < mp2.getNumUpvotes()) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }
}
