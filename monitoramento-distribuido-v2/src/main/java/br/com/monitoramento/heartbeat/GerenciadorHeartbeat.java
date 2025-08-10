package br.com.monitoramento.heartbeat;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import br.com.monitoramento.modelos.StatusGlobal;
import br.com.monitoramento.eleicao.EleicaoBully;

/**
 * Envia PINGs e marca nós inativos após 3 falhas.
 * Se detectar que o coordenador (leader) está inativo, dispara a eleição.
 */
public class GerenciadorHeartbeat {
    private final Map<Integer, InetSocketAddress> pares;
    private final ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor();
    private final Map<Integer, Integer> contagemFalhas = new ConcurrentHashMap<>();
    private final StatusGlobal global;
    private final int intervaloMs;
    private final int maxFalhas = 3;
    private final EleicaoBully eleicao;
    private volatile int coordenadorId = -1;

    public GerenciadorHeartbeat(Map<Integer, InetSocketAddress> pares, StatusGlobal global, int intervaloMs, EleicaoBully eleicao) {
        this.pares = pares;
        this.global = global;
        this.intervaloMs = intervaloMs;
        this.eleicao = eleicao;
        for (Integer id : pares.keySet()) contagemFalhas.put(id, 0);
    }

    public void setCoordenador(int id) { this.coordenadorId = id; }

    public void iniciar() {
        agendador.scheduleAtFixedRate(this::tick, 0, intervaloMs, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        for (Map.Entry<Integer, InetSocketAddress> e : pares.entrySet()) {
            int id = e.getKey();
            InetSocketAddress addr = e.getValue();
            boolean ok = ping(addr);
            if (ok) {
                contagemFalhas.put(id, 0);
                global.marcarVivo(id, true);
            } else {
                int falhas = contagemFalhas.getOrDefault(id, 0) + 1;
                contagemFalhas.put(id, falhas);
                if (falhas >= maxFalhas) {
                    global.marcarVivo(id, false);
                    // se for coordenador, iniciar eleição
                    if (id == coordenadorId) {
                        System.out.println("Coordenador perdido segundo heartbeat -> iniciando eleição");
                        eleicao.iniciarEleicao();
                    }
                }
            }
        }
    }

    private boolean ping(InetSocketAddress addr) {
        try (Socket s = new Socket()) {
            s.connect(addr, 1000);
            OutputStream os = s.getOutputStream();
            os.write(("PING\n").getBytes());
            os.flush();
            s.setSoTimeout(1000);
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String resp = r.readLine();
            return "PONG".equals(resp);
        } catch (IOException ex) {
            return false;
        }
    }

    public void shutdown() { agendador.shutdownNow(); }
}
