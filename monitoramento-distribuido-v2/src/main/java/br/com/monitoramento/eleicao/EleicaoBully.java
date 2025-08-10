package br.com.monitoramento.eleicao;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementação Bully que envia mensagens ELECTION / OK / COORDENADOR via TCP.
 * Usa pares como map id->InetSocketAddress.
 */
public class EleicaoBully {
    private final int meuId;
    private final Map<Integer, InetSocketAddress> pares;
    private volatile int coordenadorId = -1;
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final int timeoutMs = 2000;

    public EleicaoBully(int meuId, Map<Integer, InetSocketAddress> pares) {
        this.meuId = meuId;
        this.pares = new TreeMap<>(pares);
    }

    public int getCoordenadorId() { return coordenadorId; }

    public void iniciarEleicao() {
        System.out.println("[Eleicao] nó " + meuId + " iniciando eleição");
        boolean existeMaior = false;
        for (Integer id : pares.keySet()) {
            if (id > meuId) {
                existeMaior = true;
                enviarMensagem(id, "ELEICAO:" + meuId);
            }
        }
        if (!existeMaior) {
            anunciarCoordenador();
        } else {
            exec.submit(() -> {
                try { Thread.sleep(timeoutMs); } catch (InterruptedException ignored){}
                // se ninguém anunciou, assumir
                anunciarCoordenador();
            });
        }
    }

    private void anunciarCoordenador() {
        coordenadorId = meuId;
        System.out.println("[Eleicao] nó " + meuId + " é o novo coordenador");
        for (Integer id : pares.keySet()) {
            enviarMensagem(id, "COORDENADOR:" + meuId);
        }
    }

    public void tratarMensagem(String msg) {
        String[] partes = msg.split(":");
        if (partes.length < 2) return;
        String tipo = partes[0];
        int remetente = Integer.parseInt(partes[1]);
        switch (tipo) {
            case "ELEICAO":
                enviarMensagem(remetente, "OK:" + meuId);
                if (meuId > remetente) iniciarEleicao();
                break;
            case "OK":
                // alguém maior respondeu
                break;
            case "COORDENADOR":
                coordenadorId = remetente;
                System.out.println("[Eleicao] novo coordenador recebido: " + coordenadorId);
                break;
        }
    }

    private void enviarMensagem(int destId, String msg) {
        InetSocketAddress addr = pares.get(destId);
        if (addr == null) return;
        exec.submit(() -> {
            try (Socket s = new Socket()) {
                s.connect(addr, timeoutMs);
                OutputStream os = s.getOutputStream();
                os.write((msg + "\n").getBytes());
                os.flush();
                // try read ack
                s.setSoTimeout(1000);
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                r.readLine();
            } catch (IOException e) {
                // ignore; heartbeat handles failures
            }
        });
    }

    public void shutdown() { exec.shutdownNow(); }
}
