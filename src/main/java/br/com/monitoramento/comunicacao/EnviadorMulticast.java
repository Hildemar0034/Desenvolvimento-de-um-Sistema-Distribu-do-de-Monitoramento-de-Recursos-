package br.com.monitoramento.comunicacao;

import java.net.*;
import java.io.*;

/**
 * Envia mensagens via multicast UDP para clientes.
 * O líder deve validar token antes de enviar; esta classe apenas envia.
 */
public class EnviadorMulticast {
    private final InetAddress grupo;
    private final int porta;
    private final DatagramSocket socket;

    public EnviadorMulticast(String ipGrupo, int porta) throws IOException {
        this.grupo = InetAddress.getByName(ipGrupo);
        this.porta = porta;
        this.socket = new DatagramSocket();
    }

    public void enviar(String mensagem) {
        try {
            byte[] dados = mensagem.getBytes();
            DatagramPacket p = new DatagramPacket(dados, dados.length, grupo, porta);
            socket.send(p);
        } catch (IOException ignored) {}
    }

    public void fechar() { socket.close(); }
}
