package br.com.monitoramento.comunicacao;

import br.com.monitoramento.modelos.StatusNo;

import java.net.*;
import java.io.*;

/**
 * Cliente RPC simples para obter status remoto.
 */
public class RpcClient {
    private final String host;
    private final int porta;
    private final int timeoutMs = 1500;

    public RpcClient(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public StatusNo getStatus(String token) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, porta), timeoutMs);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            w.write("GET_STATUS:" + token + "\n");
            w.flush();
            String line = r.readLine();
            if (line == null) return null;
            if (line.startsWith("STATUS:")) {
                String payload = line.substring("STATUS:".length());
                return StatusNo.deserialize(payload);
            }
        } catch (IOException e) {
            // treat as unreachable
        }
        return null;
    }
}
