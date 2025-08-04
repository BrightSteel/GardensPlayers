package com.gardensmc.gardensplayers;

import java.util.Optional;

public record ActionResult(boolean isSuccess, String message) {

    public static ActionResult success() {
        return new ActionResult(true, "Success");
    }

    public static ActionResult failure(String message) {
        return new ActionResult(false, message);
    }
}
