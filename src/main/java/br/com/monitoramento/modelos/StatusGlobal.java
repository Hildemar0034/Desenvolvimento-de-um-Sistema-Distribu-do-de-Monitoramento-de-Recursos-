package br.com.monitoramento.modelos;

import java.util.concurrent.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agrega status de todos os nós e reachability.
 */
public class StatusGlobal {
    private final int meuId;
    private final ConcurrentMap<Integer, StatusNo> statusMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Boolean> vivos = new ConcurrentHashMap<>();

    public StatusGlobal(int meuId) { this.meuId = meuId; }

    public void atualizarStatus(int id, StatusNo s) { if (s!=null) { statusMap.put(id, s); vivos.put(id, true); } }
    public StatusNo getStatusLocal() { return statusMap.get(meuId); }
    public Map<Integer, StatusNo> snapshot() { return new HashMap<>(statusMap); }
    public void marcarVivo(int id, boolean vivo) { vivos.put(id, vivo); if (!vivo) statusMap.remove(id); }
    public List<Integer> nosVivos() { return vivos.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList()); }
}
