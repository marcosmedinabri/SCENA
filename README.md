# AgenteTareas — Recomendador Semántico de Cine (SMA con JADE)

Práctica de la asignatura **Sistemas de Inteligencia Artificial (SSII)** — 8.º cuatrimestre, Grado en Ingeniería Informática, UPM.

Sistema multi-agente desarrollado con el framework **JADE (Java Agent Development Framework)** que recomienda películas personalizadas consultando APIs externas y aplicando un algoritmo de puntuación semántica.

---

## Descripción

El sistema está compuesto por cinco agentes que se comunican siguiendo el protocolo **FIPA**. El usuario selecciona géneros, año y preferencia de temática a través de una interfaz gráfica; el sistema consulta dos fuentes de datos en paralelo, fusiona los resultados, los puntúa y devuelve un ranking con pósters.

### Agentes

| Agente | Clase | Rol |
|---|---|---|
| `Planificador` | `AgentePlanificador.java` | Coordina la consulta a los agentes de datos, fusiona resultados y aplica el algoritmo de ranking |
| `AgenteTMDB` | `AgenteTMDB.java` | Consulta la API de TMDB para obtener películas por género y año |
| `AgenteTrakt` | `AgenteTrakt.java` | Consulta la API de Trakt.tv para ampliar el catálogo de películas |
| `AgenteOMDB` | `AgenteOMDB.java` | Consulta la API de OMDB para obtener la URL del póster de cada película |
| `InterfazUsuario` | `AgenteInterfaz.java` | Gestiona la GUI Swing, envía los filtros al planificador y muestra el ranking |

### Flujo de comunicación

```
InterfazUsuario
    │
    ├─ 1. Usuario introduce filtros (géneros, año, preferencia)
    ├─ 2. Busca "planificacion" en el Directory Facilitator (DF)
    └─ 3. REQUEST → Planificador (objeto FiltrosUsuario serializado)
              │
              ├─ 4. REQUEST → AgenteTMDB  (filtros)
              ├─ 4. REQUEST → AgenteTrakt (filtros)
              │         │
              │         └─ 5. INFORM ← AgenteTMDB / AgenteTrakt (listas de películas)
              │
              ├─ 6. Fusiona resultados y aplica AlgoritmoPlanificador
              └─ 7. INFORM → InterfazUsuario (ArrayList<Pelicula> ordenado)
                        │
                        ├─ 8. Muestra ranking en la GUI
                        └─ 9. REQUEST → AgenteOMDB × N (título de cada película)
                                  │
                                  └─ 10. INFORM ← AgenteOMDB (URL del póster)
                                            │
                                            └─ 11. GUI actualiza póster de forma asíncrona
```

### Algoritmo de puntuación (`AlgoritmoPlanificador`)

Cada película recibe un score compuesto por tres factores ponderados:

| Factor | Peso | Fuente |
|---|---|---|
| Coincidencia de géneros | 5.0 | Géneros de la película vs. géneros seleccionados por el usuario |
| Valoración TMDB | 3.0 | `vote_average` (0–10) normalizado |
| Palabras clave en sinopsis | 1.5 | Coincidencias entre la preferencia del usuario y la descripción |

---

## Requisitos

- **Java** 11 o superior (probado con JavaSE-21)
- **Maven** 3.6+
- Las librerías en `AgenteTareas/lib/` deben estar presentes:
  - `jade.jar` (v4.6.0)
  - `commons-codec-1.3.jar`
  - `gson-2.14.0.jar`

---

## Estructura del proyecto

```
SSII/
├── AgenteTareas/
│   ├── lib/
│   │   ├── jade.jar                      # Framework JADE 4.6.0
│   │   ├── commons-codec-1.3.jar         # Dependencia de JADE
│   │   └── gson-2.14.0.jar               # Parseo JSON (Trakt)
│   ├── src/es/upm/si/practica/
│   │   ├── Main.java                     # Punto de entrada; crea el contenedor y lanza los 5 agentes
│   │   ├── AgentePlanificador.java       # Coordinador: consulta agentes de datos y genera ranking
│   │   ├── AgenteTMDB.java               # Fuente de datos: API de TMDB
│   │   ├── AgenteTrakt.java              # Fuente de datos: API de Trakt.tv
│   │   ├── AgenteOMDB.java               # Fuente de pósters: API de OMDB
│   │   ├── AgenteInterfaz.java           # GUI + cliente del sistema
│   │   ├── BuscadorGUI.java              # Ventana Swing del recomendador
│   │   ├── AlgoritmoPlanificador.java    # Lógica de scoring y ranking
│   │   ├── PalabrasClavePlanificador.java# Extracción de keywords de la preferencia
│   │   ├── FiltrosUsuario.java           # Objeto serializable con los filtros del usuario
│   │   ├── Pelicula.java                 # Modelo de película (nombre, géneros, puntuación, sinopsis)
│   │   ├── PeliculaTMDB.java             # Mapeo JSON de respuesta TMDB
│   │   └── ConjuntoTMDB.java             # Envoltorio de lista de resultados TMDB
│   └── pom.xml                           # Configuración Maven
├── compilar_ejecutar.bat                 # Script de compilación y arranque (Windows)
└── README.md
```

---

## Compilación y ejecución

### Con el script (recomendado — Windows)

```bat
compilar_ejecutar.bat
```

El script compila con Maven y lanza el sistema directamente con el classpath correcto. Los agentes se comunican por el **puerto 1202**.

### Manual

```bash
cd AgenteTareas
mvn compile
java -cp "target/classes;lib/jade.jar;lib/commons-codec-1.3.jar;lib/gson-2.14.0.jar" es.upm.si.practica.Main
```

> **Nota:** `mvn exec:java` no funciona con dependencias de ámbito `system` (las jars locales). Usar siempre el classpath manual o el script.

### Con Eclipse

1. `File > Import > Existing Projects into Workspace`
2. Seleccionar la carpeta `AgenteTareas/`
3. Ejecutar `Main.java` como aplicación Java

---

## APIs externas

| API | Uso | Autenticación |
|---|---|---|
| [TMDB](https://www.themoviedb.org/documentation/api) | Películas por género y año, valoración | API key en `AgenteTMDB.java` |
| [Trakt.tv](https://trakt.docs.apiary.io/) | Catálogo alternativo de películas | Client ID en `AgenteTrakt.java` |
| [OMDB](https://www.omdbapi.com/) | URL del póster por título | API key en `AgenteOMDB.java` |

---

## Detalles técnicos

### Registro en el Directory Facilitator

Cada agente de servicio se registra al iniciarse con su tipo:

| Tipo de servicio | Agente |
|---|---|
| `"planificacion"` | `AgentePlanificador` |
| `"busqueda-peliculas-tmdb"` | `AgenteTMDB` |
| `"busqueda-peliculas-trakt"` | `AgenteTrakt` |
| `"busqueda-posters"` | `AgenteOMDB` |

`AgenteInterfaz` los descubre dinámicamente en el DF sin conocer sus AIDs de antemano.

### Comportamientos JADE utilizados

| Comportamiento | Agente | Descripción |
|---|---|---|
| `CyclicBehaviour` | Planificador, TMDB, Trakt, OMDB | Atienden peticiones de forma continua |
| `OneShotBehaviour` | AgenteInterfaz | Ejecuta una consulta completa por búsqueda del usuario |
| `CyclicBehaviour` | AgenteInterfaz | Recibe respuestas de póster de AgenteOMDB asíncronamente |

### Gestión de pósters

Los pósters se cargan de forma asíncrona para no bloquear la GUI:
1. `AgenteInterfaz` envía un `REQUEST` a `AgenteOMDB` por cada película del ranking, usando `conversation-id` del formato `poster-{timestamp}-{i}`.
2. `AgenteOMDB` responde con `INFORM` (URL del póster) o `FAILURE`.
3. El `CyclicBehaviour` de `AgenteInterfaz` recibe los `INFORM` y llama a `BuscadorGUI.actualizarPosterDesdeUrl()`.
4. `BuscadorGUI` descarga y escala la imagen en un `SwingWorker` para no bloquear el EDT.

---

## Autores

- **Marcos**
- **Diego**
- **Silvia**
- **Daniel**

---

## Licencia

Proyecto académico — uso educativo.
