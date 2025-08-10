package br.com.monitoramento.autenticacao;

import java.util.*;
import java.util.concurrent.*;

/**
 * Serviço simples de autenticação: login -> token UUID.
 * Tokens armazenados em memória com TTL.
 */
public class ServicoAutenticacao {
    private final Map<String, String> usuarios = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();
    private final Map<String, Long> criado = new ConcurrentHashMap<>();
    private final long ttlMs = 24 * 3600 * 1000L;

    public ServicoAutenticacao() {
        // usuário padrão para testes
        usuarios.put("admin", "admin");
    }

    public Optional<String> login(String usuario, String senha) {
        String p = usuarios.get(usuario);
        if (p != null && p.equals(senha)) {
            String token = UUID.randomUUID().toString();
            tokenToUser.put(token, usuario);
            criado.put(token, System.currentTimeMillis());
            return Optional.of(token);
        }
        return Optional.empty();
    }

    public boolean validar(String token) {
        if (token == null) return false;
        String u = tokenToUser.get(token);
        if (u == null) return false;
        Long c = criado.get(token);
        if (c == null) return false;
        if (System.currentTimeMillis() - c > ttlMs) {
            tokenToUser.remove(token);
            criado.remove(token);
            return false;
        }
        return true;
    }

    public void revogar(String token) {
        tokenToUser.remove(token);
        criado.remove(token);
    }

    // helper para testes: retorna um token válido para 'admin'
    public String tokenTeste() {
        return login("admin", "admin").orElseThrow();
    }
}
