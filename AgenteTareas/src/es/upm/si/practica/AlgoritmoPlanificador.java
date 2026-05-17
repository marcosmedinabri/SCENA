package es.upm.si.practica;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoPlanificador {

    // Capacidad de trabajo por día
    private static final int HORAS_DIARIAS_MAX = 7;

    public static String generarHorarioSemanal(List<Tarea> tareasRecibidas) {
        if (tareasRecibidas == null || tareasRecibidas.isEmpty()) {
            return "No hay tareas para planificar. ¡Disfruta de tu tiempo libre!";
        }

        // 1. SANITIZACIÓN DE DATOS
        List<Tarea> tareas = new ArrayList<>();
        for (Tarea t : tareasRecibidas) {
            if (t.getNombre() != null && t.getHorasDedicadas() > 0 && t.getDeadline() != null) {
                tareas.add(t);
            }
        }

        if (tareas.isEmpty()) {
            return "Error: Las tareas recibidas contienen datos inválidos.";
        }

        // Día de hoy para calcular la urgencia
        final Calendar hoy = Calendar.getInstance();

        // 2. ORDENACIÓN HEURÍSTICA (Priorizando Quick Wins y urgencia)
        Collections.sort(tareas, new Comparator<Tarea>() {
            @Override
            public int compare(Tarea t1, Tarea t2) {
                double urgencia1 = calcularUrgencia(t1, hoy);
                double urgencia2 = calcularUrgencia(t2, hoy);
                
                // Orden descendente: el que tenga mayor urgencia va primero
                return Double.compare(urgencia2, urgencia1);
            }
        });

        // 3. SIMULACIÓN DEL CALENDARIO Y REPARTO
        StringBuilder plan = new StringBuilder();
        plan.append("\n=== PLANIFICACIÓN INTELIGENTE (SCORE COMBINADO) ===\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Calendar calendarioActual = Calendar.getInstance();
        int horasDisponiblesHoy = HORAS_DIARIAS_MAX;

        for (Tarea tarea : tareas) {
            int horasRestantes = tarea.getHorasDedicadas();
            boolean tareaCompletada = false;

            // Mostramos el cálculo en el log para ver el Score
            double score = calcularUrgencia(tarea, hoy);
            plan.append(String.format("\n>> Tarea: '%s' | Urgencia: %.4f | Faltan %dh | Límite: %s\n", 
                        tarea.getNombre(), score, horasRestantes, sdf.format(tarea.getDeadline())));

            while (horasRestantes > 0) {
                // Validación de si el tiempo nos ha atropellado
                Calendar fechaLimiteAux = Calendar.getInstance();
                fechaLimiteAux.setTime(tarea.getDeadline());
                // Sumamos 1 para que deje trabajar en el mismo día del deadline
                fechaLimiteAux.add(Calendar.DAY_OF_MONTH, 1); 

                if (calendarioActual.after(fechaLimiteAux)) {
                    plan.append("   ⚠️ ¡ALERTA! Imposible terminar a tiempo. Rebasada la fecha límite.\n");
                    break; 
                }

                int horasAsignadasHoy = Math.min(horasRestantes, horasDisponiblesHoy);
                
                plan.append("   - ").append(sdf.format(calendarioActual.getTime()))
                    .append(": Dedicar ").append(horasAsignadasHoy).append("h.\n");

                horasRestantes -= horasAsignadasHoy;
                horasDisponiblesHoy -= horasAsignadasHoy;

                if (horasDisponiblesHoy == 0) {
                    calendarioActual.add(Calendar.DAY_OF_MONTH, 1);
                    horasDisponiblesHoy = HORAS_DIARIAS_MAX;
                }
                
                if (horasRestantes == 0) {
                    tareaCompletada = true;
                }
            }

            if (tareaCompletada) {
                plan.append("   ✅ Tarea planificada con éxito.\n");
            }
        }

        plan.append("\n===================================================\n");
        return plan.toString();
    }

    /**
     * Calcula la urgencia de una tarea combinando criticidad, horas necesarias y tiempo restante.
     * FÓRMULA ACTUALIZADA: Prioriza tareas cortas, importantes y cercanas a la fecha límite.
     */
    private static double calcularUrgencia(Tarea t, Calendar hoy) {
        // 1. Calcular días restantes
        long diferenciaMillis = t.getDeadline().getTime() - hoy.getTimeInMillis();
        long diasRestantes = diferenciaMillis / (1000 * 60 * 60 * 24);
        
        // Si el deadline es hoy o ya pasó, forzamos el divisor a 1 para no dividir por 0
        if (diasRestantes < 1) {
            diasRestantes = 1;
        }

        // 2. Invertir la criticidad (1 -> 5 de peso, 5 -> 1 de peso)
        int pesoCriticidad = 6 - t.getCriticidad();
        if (pesoCriticidad < 1) pesoCriticidad = 1;
        if (pesoCriticidad > 5) pesoCriticidad = 5;

        // 3. Aplicar la fórmula del "Bot Calculador" (Quick Wins)
        // Dividimos entre las horas requeridas para que las tareas cortas tengan un plus de urgencia
        return (double) pesoCriticidad / (diasRestantes * t.getHorasDedicadas());
    }
}