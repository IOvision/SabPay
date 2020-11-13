package com.visionio.sabpay.models;

import java.util.ArrayList;
import java.util.List;

public class TagsCategory {
    String id;
    ArrayList<String> tags;
    String image;

    @Override
    public String toString() {
        StringBuilder a = new StringBuilder("ID = " + id + " ,tags =");
        for (String temp : tags) {
            a.append(" ").append(temp);
        }
        return a.toString();
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public String getId() {
        return id;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public void setId(String id) {
        this.id = id;
    }
}
