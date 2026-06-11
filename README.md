```
               ⢹⡶⠶⣶⡶⠶⣶⡆      
      .--.     ⢸⣠⡾⠋⣠⡾⠋⡇      
     /    \    ⢸⠿⠶⠾⠿⠶⠾⡇      
    ## a  a    ⢸ ⠀⠀⠀⠀⠀⠀⡇     
     ( '._ )   ⢸⣀⣀⣀⣀⣀⣀⡇      
     | '-- |   ⠈⠉⠉⠉⠉⠉⠉⠁      d888888o.       ,o888888o.    8 8888888888   b.             8          .8.         
    _.\___/_   __________     .`8888:' `88.    8888     `88.  8 8888         888o.          8         .888.        
  ."\> \Y/|<'.  '._.-'        8.`8888.   Y8 ,8 8888       `8. 8 8888         Y88888o.       8        :88888.       
 /  \ \_\/ /  '-' /           `8.`8888.     88 8888           8 8888         .`Y888888o.    8       . `88888.      
 | --'\_/|/ |   _/             `8.`8888.    88 8888           8 888888888888 8o. `Y888888o. 8      .8. `88888.     
 |___.-' |  |`'`                `8.`8888.   88 8888           8 8888         8`Y8o. `Y88888o8     .8`8. `88888.    
   |     |  |                    `8.`8888.  88 8888           8 8888         8   `Y8o. `Y8888    .8' `8. `88888.   
   |    / './                8b   `8.`8888. `8 8888       .8' 8 8888         8      `Y8o. `Y8   .8'   `8. `88888.  
  /__./` | |                 `8b.  ;8.`8888    8888     ,88'  8 8888         8         `Y8o.`  .888888888. `88888. 
     \   | |                  `Y8888P ,88P'     `8888888P'    8 888888888888 8            `Yo .8'       `8. `88888.
      \  | |                 
      ;  | |                 
      /  | |                 
     |___\_.\_               
     `-"--'---'              
```

# Sistema Multiagente Distribuido Inteligente para Recomendación de Películas
# SCENA (Smart Cooperative Engine for Networked Agents)
---
## 1. Instrucciones de instalación
En el caso de nuestro proyecto hemos usado JADE sobre el entorno de desarrollo integrado de Eclipse.
Para instalar el entorno se han de seguir los siguientes pasos (basados en el material dado en la asignatura):
1. **Descargar Java:** Tienes que tener instalado la versión de java 11 instalado en tu equipo.
2. **Descargar el proyecto:** Necesitas descargar el proyecto a través del repositorio marcosmedinabri/SSII (lo puedes hacer descargando el .zip y descomprimiéndolo o con el mandato git clone, a preferencia de cada uno).
3. **Importar el proyecto:** Importa la carpeta que se llama "AgenteTarea" en tu IDE favorito como "Maven Project". Asegúrate de que tu IDE ha importado correctamente el pom.xml con sus dependencias y versión de java especificada (11).
4. **Obtener claves APIs:** Por motivos de seguridad y privacidad, necesitas obtener tus propias api keys de los siguientes lugares y colocarlas en los siguientes agentes:
  * OMDb API - The Open Movie Database colocando el api en AgenteOMDB.
  * The Movie Database (TMDB) colocando el api en AgenteTMDB.
  * TRAK.TV colocando el api en AgenteTrakt.
---
## 2. Captura de dependencias necesarias para instalar el proyecto
<img width="447" height="379" alt="image" src="https://github.com/user-attachments/assets/2d8b8f91-b624-438c-921f-578ca89c87bc" />
---

## 3. Instrucciones de ejecución

Si has seguido correctamente los pasos de instalación, desde tu IDE solo necesitas ejecutar el Main.java y desde ahí se cargará el entorno Jade con todos sus agentes respectivos.
En la ventana grafica que se te abre, selecciona los géneros de películas que deseas ver, el año mínimo de las películas, una breve descripción de lo que te gustaría ver y finalmente dale al botón de buscar, espera unos segundos y si todo ha salido correctamente veras un listado de películas.
---

## 4. Datos de ejemplo para ejecutar la práctica
Como los datos que introduce el usuario son por ventana gráfica, especificamos que deberia poner el usuario en cada campo para comprobarlo.
**Datos de ejemplo para ejecutar la práctica:**
### 1.
* **Generos:** acción, comedia
* **Año:** 2000
* **Descripción:** De robots y animales
### 2.
* **Generos:** historia
* **Año:** 2015
* **Descripción:** Quiero ver de Marvel
### 3.
* **Generos:** crimen, terror
* **Año:** 2010
* **Descripción:** Búscame de misterio y también de asesinatos
---
## 5. Un diagrama de la arquitectura del sistema
<img width="925" height="429" alt="image" src="https://github.com/user-attachments/assets/d84eda5d-6b00-48a3-8c6f-05d5df62f16b" />
---

## 6. Una declaración de IA, indicando cómo se ha utilizado en el proyecto
El uso de la IA se ha usado para entender conceptos de Jade que no teníamos del todo claro al principio, resolver problemas de código donde no sabíamos que estaba fallando para que nos guiara y buscar información de como funcionaban las APIs.
Adicionalmente, se ha empleado la IA para generar la imagen de la arquitectura estilizando una imagen generada por nosotros a partir de iconos.
