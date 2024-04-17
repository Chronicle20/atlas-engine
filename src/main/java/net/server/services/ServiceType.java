package net.server.services;

public interface ServiceType<T extends Enum<?>> {
    Service createService();

    int ordinal();

    T[] enumValues();
}
