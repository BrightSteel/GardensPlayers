package com.gardensmc.gardensplayers.command.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PlayerNotFoundException extends IllegalArgumentException {
    private final String username;
}
