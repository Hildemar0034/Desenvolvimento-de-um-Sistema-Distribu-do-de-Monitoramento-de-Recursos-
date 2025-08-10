package br.com.monitoramento.comunicacao;

import br.com.monitoramento.modelos.StatusNo;
import br.com.monitoramento.modelos.StatusGlobal;
import br.com.monitoramento.autenticacao.ServicoAutenticacao;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

/**
 * Servidor RPC simples (TCP) que responde ao comando:
 *   GET_STATUS:<token>
 * Retorna: STATUS:<serializedStatus> ou ERROR:REASON
 */
public class RpcServer {
    private final int porta;
    private final ServicoAutenticacao auth;
    private final StatusGlobal global;
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public RpcServer(int porta, ServicoAutenticacao auth, StatusGlobal global) {
        this.porta = porta;
        this.auth = auth;
        this.global = global;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(porta);
        exec.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket s = serverSocket.accept();
                    exec.submit(() -> handle(s));
                } catch (IOException ignored) {}
            }
        });
    }

    private void handle(Socket s) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            String line = r.readLine();
            if (line == null) return;
            if (line.startsWith("GET_STATUS:")) {
                String token = line.substring("GET_STATUS:".length());
                if (!auth.validar(token)) {
                    w.write("ERROR:INVALID_TOKEN\n");
                    w.flush();
                    return;
                }
                StatusNo local = global.getStatusLocal();
                if (local == null) {
                    w.write("ERROR:NO_STATUS\n");
                } else {
                    w.write("STATUS:" + local.serialize() + "\n");
                }
                w.flush();
            } else {
                w.write("ERROR:UNKNOWN_COMMAND\n");
                w.flush();
            }
        } catch (IOException ignored) {}
    }

    public void stop() throws IOException {
        if (serverSocket != null) serverSocket.close();
        exec.shutdownNow();
    }
}
