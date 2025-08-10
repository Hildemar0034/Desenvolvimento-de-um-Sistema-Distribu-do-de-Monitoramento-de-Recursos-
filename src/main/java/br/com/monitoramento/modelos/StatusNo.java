package br.com.monitoramento.modelos;

import java.time.Instant;

/**
 * Representa o estado de um nó.
 */
public class StatusNo {
    public final int idNo;
    public final double cargaCpu;
    public final long memoriaUsada;
    public final long uptimeSegundos;
    public final long lamport;
    public final Instant coletadoEm;

    public StatusNo(int idNo, double cargaCpu, long memoriaUsada, long uptimeSegundos, long lamport) {
        this.idNo = idNo;
        this.cargaCpu = cargaCpu;
        this.memoriaUsada = memoriaUsada;
        this.uptimeSegundos = uptimeSegundos;
        this.lamport = lamport;
        this.coletadoEm = Instant.now();
    }

    @Override
    public String toString() {
        return "StatusNo{" +
                "idNo=" + idNo +
                ", cargaCpu=" + cargaCpu +
                ", memoriaUsada=" + memoriaUsada +
                ", uptimeSegundos=" + uptimeSegundos +
                ", lamport=" + lamport +
                ", coletadoEm=" + coletadoEm +
                '}';
    }

    // Serializa o estado para string
    public String serialize() {
        return idNo + "|" + cargaCpu + "|" + memoriaUsada + "|" + uptimeSegundos + "|" + lamport + "|" + coletadoEm.toString();
    }

    // Desserializa de forma robusta
    public static StatusNo deserialize(String s) {
        if (s == null) return null;
        try {
            String[] p = s.split("\\\\|", 6);
            if (p.length < 5) return null;
            int id = Integer.parseInt(p[0].trim());
            double cpu = Double.parseDouble(p[1].trim());
            long mem = Long.parseLong(p[2].trim());
            long up = Long.parseLong(p[3].trim());
            long lam = Long.parseLong(p[4].trim());
            return new StatusNo(id, cpu, mem, up, lam);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
