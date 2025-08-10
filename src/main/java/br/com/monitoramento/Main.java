package br.com.monitoramento;

import br.com.monitoramento.autenticacao.ServicoAutenticacao;
import br.com.monitoramento.eleicao.EleicaoBully;
import br.com.monitoramento.modelos.StatusGlobal;
import br.com.monitoramento.sincronizacao.RelogioLamport;
import br.com.monitoramento.utils.ColetorRecursos;
import br.com.monitoramento.heartbeat.GerenciadorHeartbeat;
import br.com.monitoramento.comunicacao.ServidorSocket;
import br.com.monitoramento.comunicacao.EnviadorMulticast;
import br.com.monitoramento.comunicacao.RpcServer;
import br.com.monitoramento.comunicacao.RpcClient;
import br.com.monitoramento.sincronizacao.GerenciadorSnapshot;
import br.com.monitoramento.modelos.StatusNo;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main que inicializa um nó com argumentos:
 *   java ... Main <meuId> <portaBase> "<peerId:host:porta,peerId2:host:porta,...>"
 * Exemplo para testes locais (hosts localhost, portas 5000,5001,5002):
 *   java ... Main 1 5000 "2:localhost:5001,3:localhost:5002"
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: Main <meuId> <portaBase> \"<peerId:host:porta,peer2:host:porta,...>\"");
            System.out.println("Exemplo local: Main 1 5000 \"2:localhost:5001,3:localhost:5002\"");
            return;
        }
        int meuId = Integer.parseInt(args[0]);
        int portaBase = Integer.parseInt(args[1]);
        String peersArg = args[2];

        Map<Integer, InetSocketAddress> peers = new HashMap<>();
        Map<Integer, RpcClient> rpcClients = new HashMap<>();
        if (!peersArg.trim().isEmpty()) {
            String[] parts = peersArg.split(",");
            for (String p : parts) {
                String[] t = p.split(":");
                if (t.length != 3) continue;
                int id = Integer.parseInt(t[0]);
                String host = t[1];
                int port = Integer.parseInt(t[2]);
                peers.put(id, new InetSocketAddress(host, port));
                rpcClients.put(id, new RpcClient(host, port + 100)); // RPC server port = base + 100 (convention)
            }
        }

        StatusGlobal global = new StatusGlobal(meuId);
        RelogioLamport clock = new RelogioLamport();
        ColetorRecursos coletor = new ColetorRecursos(meuId, clock);
        ServicoAutenticacao auth = new ServicoAutenticacao();
        String tokenTeste = auth.tokenTeste(); // token para uso em testes

        // coletar e registrar local inicialmente
        StatusNo inicial = coletor.coletar();
        global.atualizarStatus(meuId, inicial);

        // eleicao
        // pares para eleicao usam porto base (not RPC port)
        Map<Integer, InetSocketAddress> paresEleicao = new HashMap<>();
        for (Map.Entry<Integer, InetSocketAddress> e : peers.entrySet()) {
            paresEleicao.put(e.getKey(), new InetSocketAddress(e.getValue().getHostString(), e.getValue().getPort()));
        }
        EleicaoBully eleicao = new EleicaoBully(meuId, paresEleicao);

        // heartbeat
        GerenciadorHeartbeat hb = new GerenciadorHeartbeat(peers, global, 2000, eleicao);
        hb.iniciar();

        // servidor socket (para PING/ELEICAO) - listens on portaBase
        ServidorSocket servidor = new ServidorSocket(portaBase, eleicao, global);
        servidor.iniciar();

        // rpc server on portaBase+100
        RpcServer rpcServer = new RpcServer(portaBase + 100, auth, global);
        rpcServer.start();

        // multicast sender
        EnviadorMulticast multicast = new EnviadorMulticast("230.0.0.1", 4446);

        // snapshot manager (leader will call) - uses rpcClients and tokenTeste
        GerenciadorSnapshot snap = new GerenciadorSnapshot(global, clock, rpcClients, tokenTeste);

        // schedule: collector runs locally and leader coordinates snapshot every 5s
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
        exec.scheduleAtFixedRate(() -> {
            try {
                StatusNo s = coletor.coletar();
                global.atualizarStatus(meuId, s);
                System.out.println("Coleta local: " + s);
            } catch (Exception e) { e.printStackTrace(); }
        }, 0, 5, TimeUnit.SECONDS);

        // leader task: if I'm leader, perform snapshot and multicast
        exec.scheduleAtFixedRate(() -> {
            try {
                // check who is coordinator (eleicao stores locally but we rely on eleicao.getCoordenadorId)
                int coord = eleicao.getCoordenadorId();
                if (coord == meuId) {
                    System.out.println("Sou coordenador -> orquestrando snapshot");
                    Map<Integer, StatusNo> snapMap = snap.coletarSnapshotGlobal();
                    // enviar via multicast como coordenador (token validated earlier)
                    String msg = "GLOBAL_SNAPSHOT:" + snapMap.toString();
                    multicast.enviar(msg);
                    System.out.println("Coordenador enviou multicast: " + msg);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, 3, 5, TimeUnit.SECONDS);

        // shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                exec.shutdownNow();
                hb.shutdown();
                rpcServer.stop();
                servidor.shutdown();
                multicast.fechar();
                eleicao.shutdown();
            } catch (Exception ignored) {}
        }));

        System.out.println("Nó " + meuId + " iniciado. Token de teste (admin): " + tokenTeste);
    }
}
