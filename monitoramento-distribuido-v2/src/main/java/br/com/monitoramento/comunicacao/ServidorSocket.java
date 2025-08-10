package br.com.monitoramento.comunicacao;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import br.com.monitoramento.eleicao.EleicaoBully;
import br.com.monitoramento.modelos.StatusGlobal;

/**
 * Servidor TCP que trata PING (heartbeat), mensagens de eleição (ELEICAO/OK/COORDENADOR)
 * e encaminha GET_STATUS para o RpcServer (separado).
 */
public class ServidorSocket {
    private final int porta;
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final ServerSocket serverSocket;
    private final EleicaoBully eleicao;
    private final StatusGlobal global;

    public ServidorSocket(int porta, EleicaoBully eleicao, StatusGlobal global) throws IOException {
        this.porta = porta;
        this.eleicao = eleicao;
        this.global = global;
        this.serverSocket = new ServerSocket(porta);
    }

    public void iniciar() {
        exec.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket s = serverSocket.accept();
                    exec.submit(() -> handleClient(s));
                } catch (IOException ignored) {}
            }
        });
    }

    private void handleClient(Socket s) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            String line = r.readLine();
            if (line == null) return;
            if (line.startsWith("PING")) {
                w.write("PONG\n"); w.flush(); return;
            } else if (line.startsWith("ELEICAO:") || line.startsWith("OK:") || line.startsWith("COORDENADOR:")) {
                eleicao.tratarMensagem(line);
                w.write("ACK\n"); w.flush();
                return;
            } else {
                // unknown at this level; RpcServer handles GET_STATUS on its own port
                w.write("UNKNOWN\n"); w.flush();
                return;
            }
        } catch (IOException ignored) {}
    }

    public void shutdown() throws IOException {
        serverSocket.close();
        exec.shutdownNow();
    }
}
