package br.com.monitoramento.utils;

import br.com.monitoramento.modelos.StatusNo;
import br.com.monitoramento.sincronizacao.RelogioLamport;

import java.lang.management.*;
import com.sun.management.OperatingSystemMXBean;

/**
 * Coleta métricas locais e retorna StatusNo.
 */
public class ColetorRecursos {
    private final int idNo;
    private final RelogioLamport clock;
    private final OperatingSystemMXBean os;
    private final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

    public ColetorRecursos(int idNo, RelogioLamport clock) {
        this.idNo = idNo;
        this.clock = clock;
        OperatingSystemMXBean ob = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.os = ob;
    }

    public StatusNo coletar() {
        double cpu = os.getSystemCpuLoad();
        if (cpu < 0) cpu = 0.0;
        long free = os.getFreePhysicalMemorySize();
        long total = os.getTotalPhysicalMemorySize();
        long used = Math.max(0, total - free);
        long uptime = runtime.getUptime() / 1000L;
        long lamport = clock.tick();
        return new StatusNo(idNo, cpu, used, uptime, lamport);
    }
}
