package br.com.monitoramento.sincronizacao;

import br.com.monitoramento.modelos.StatusNo;
import br.com.monitoramento.modelos.StatusGlobal;
import br.com.monitoramento.comunicacao.RpcClient;

import java.util.*;
import java.util.concurrent.*;

/**
 * Gerenciador de snapshot: líder orquestra coleta de status de pares via RPC.
 */
public class GerenciadorSnapshot {
    private final StatusGlobal global;
    private final RelogioLamport clock;
    private final Map<Integer, RpcClient> clientesRemotos;
    private final String token; // token que líder usa para consultar outros nós

    public GerenciadorSnapshot(StatusGlobal global, RelogioLamport clock, Map<Integer, RpcClient> clientesRemotos, String token) {
        this.global = global;
        this.clock = clock;
        this.clientesRemotos = clientesRemotos;
        this.token = token;
    }

    public Map<Integer, StatusNo> coletarSnapshotGlobal() {
        long localTime = clock.tick();
        Map<Integer, StatusNo> snapshot = new HashMap<>();
        StatusNo local = global.getStatusLocal();
        if (local != null) snapshot.put(local.idNo, local);

        List<CompletableFuture<Void>> futuros = new ArrayList<>();
        for (Map.Entry<Integer, RpcClient> e : clientesRemotos.entrySet()) {
            int id = e.getKey();
            RpcClient c = e.getValue();
            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                StatusNo ns = c.getStatus(token);
                if (ns != null) snapshot.put(id, ns);
            });
            futuros.add(f);
        }
        try {
            CompletableFuture.allOf(futuros.toArray(new CompletableFuture[0])).get(2500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}
        snapshot.forEach((id, ns) -> global.atualizarStatus(id, ns));
        return snapshot;
    }
}
