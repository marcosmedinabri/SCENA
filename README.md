# SSII
# AgenteTareas — Sistema Multi-Agente con JADE

Práctica de la asignatura **Sistemas de Inteligencia Artificial (SSII)** — 8.º cuatrimestre, Grado en Ingeniería en Robótica, UPM.

Sistema multi-agente desarrollado con el framework **JADE (Java Agent Development Framework)** que implementa planificación y scheduling de tareas semanales mediante comunicación entre agentes autónomos.

---

## Descripción

El sistema está compuesto por dos agentes que se comunican siguiendo el protocolo **FIPA (Foundation for Intelligent Physical Agents)**:

| Agente | Clase | Rol |
|---|---|---|
| `AgentePlanificador` | `AgentePlanificador.java` | Recibe solicitudes de planificación, procesa las tareas y devuelve un horario semanal |
| `AgenteInterfaz` | `AgenteInterfaz.java` | Punto de entrada del usuario; busca al planificador en el DF, le envía las tareas y muestra el resultado |

### Flujo de comunicación

```
AgenteInterfaz
    │
    ├─ 1. Busca "planificacion" en el Directory Facilitator (DF)
    ├─ 2. Envía mensaje REQUEST con lista de tareas
    │
    ▼
AgentePlanificador
    ├─ 3. Procesa las tareas recibidas
    └─ 4. Responde con mensaje INFORM con el horario generado
    │
    ▼
AgenteInterfaz
    └─ 5. Muestra el resultado por consola
```

---

## Requisitos

- **Java** 11 o superior (probado con JavaSE-21)
- **Maven** 3.6+
- **Eclipse IDE** (recomendado; incluye `.project` y `.classpath`)
- Las librerías en `AgenteTareas/lib/` deben estar presentes:
  - `jade.jar` (v4.6.0)
  - `commons-codec-1.3.jar`

---

## Estructura del proyecto

```
AgenteTareas/
├── lib/
│   ├── jade.jar                  # Framework JADE 4.6.0
│   └── commons-codec-1.3.jar     # Dependencia de JADE
├── src/es/upm/si/practica/
│   ├── Main.java                 # Punto de entrada; crea el contenedor y lanza los agentes
│   ├── AgentePlanificador.java   # Agente planificador (servicio de scheduling)
│   └── AgenteInterfaz.java       # Agente interfaz (cliente)
└── pom.xml                       # Configuración Maven
```

---

## Compilación y ejecución

### Con Maven

```bash
cd AgenteTareas
mvn clean compile
mvn exec:java -Dexec.mainClass="es.upm.si.practica.Main"
```

### Con Eclipse

1. Importar el proyecto: `File > Import > Existing Projects into Workspace`.
2. Seleccionar la carpeta `AgenteTareas/`.
3. Ejecutar `Main.java` como aplicación Java.

Al arrancar, JADE abre su GUI de administración en el puerto **1201**.

---

## Detalles técnicos

### Registro en el Directory Facilitator

`AgentePlanificador` se registra al iniciarse con el tipo de servicio `"planificacion"` bajo el nombre `"Servicio-Planificacion-Semanal"`. Esto permite que cualquier agente lo descubra dinámicamente sin conocer su AID (Agent Identifier) de antemano.

### Comportamientos JADE utilizados

| Comportamiento | Agente | Descripción |
|---|---|---|
| `CyclicBehaviour` | `AgentePlanificador` | Escucha continuamente mensajes REQUEST y responde con INFORM |
| `OneShotBehaviour` | `AgenteInterfaz` | Ejecuta una única solicitud de planificación y termina |

### Mensajes FIPA

- **REQUEST** — enviado por `AgenteInterfaz` con el contenido de las tareas.
- **INFORM** — respondido por `AgentePlanificador` con el horario resultante.
- `conversation-id` para rastrear la conversación: `"planificacion-semana-1"`.

---

## Autores

- **Marcos Medina** (`marcosmedinabri`)
- **Diego** 
- **Silvia**

---

## Licencia

Proyecto académico — uso educativo.
