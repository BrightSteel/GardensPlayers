package com.gardensmc.gardensplayers.database.entry.nested;

import com.google.gson.Gson;

public abstract class JsonEntry {

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}