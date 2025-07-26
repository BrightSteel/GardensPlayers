package com.gardensmc.gardensplayers.command.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class InvalidUserException extends IllegalStateException {

    public InvalidUserException(String m) {
        super(m);
    }
}
