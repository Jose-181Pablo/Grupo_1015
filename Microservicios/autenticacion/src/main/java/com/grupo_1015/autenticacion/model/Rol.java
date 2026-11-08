package com.grupo_1015.autenticacion.model;

// Quien es la persona dentro del sistema.
// El INVITADO no se guarda: es quien hace peticiones sin token.
public enum Rol {
    ADMIN,
    USUARIO
}
