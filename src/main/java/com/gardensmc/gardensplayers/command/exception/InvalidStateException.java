package com.gardensmc.gardensplayers.command.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvalidStateException extends IllegalStateException {
    private String reason;
}