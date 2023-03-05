package org.honey.telegram_bot_be.services;

public interface Sender<T1, T2> {
    void invoke(T1 t1, T2 t2);
}
