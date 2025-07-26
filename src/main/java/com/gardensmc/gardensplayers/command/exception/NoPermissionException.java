package com.gardensmc.gardensplayers.command.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NoPermissionException extends RuntimeException {
    private final String permissionNode;
}
