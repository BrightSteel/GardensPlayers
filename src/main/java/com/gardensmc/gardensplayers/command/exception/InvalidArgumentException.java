package com.gardensmc.gardensplayers.command.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvalidArgumentException extends IllegalArgumentException {
    private String argumentName;
}
