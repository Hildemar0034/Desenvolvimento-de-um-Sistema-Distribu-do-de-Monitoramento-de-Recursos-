package br.com.monitoramento.sincronizacao;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Relógio lógico de Lamport.
 */
public class RelogioLamport {
    private final AtomicLong time = new AtomicLong(0);

    public long tick() {
        return time.incrementAndGet();
    }

    public long aoReceber(long recebido) {
        long atual = time.get();
        long atualizado = Math.max(atual, recebido);
        atualizado = Math.max(atualizado, time.incrementAndGet());
        time.set(atualizado);
        return atualizado;
    }

    public long getTime() { return time.get(); }
    public void setTime(long t) { time.set(t); }
}
