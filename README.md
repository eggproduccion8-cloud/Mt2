# Mundo de Tronos 2 — Actualización Mayor (Forge 1.20.1 / Java 17)

**Mundo de Tronos 2** es un mod oficial de Minecraft diseñado para servidores de alta capacidad (70–100+ jugadores concurrentes) con cero sobrecarga de TPS, persistencia real en disco, interfaces nativas medievales interactivas en español, sistema de roles/clases, asedios con cargas de asalto y comandos de gestión avanzados.

---

## 🌟 Características Principales

### 1. HUD Principal Rediseñado (Esquina Superior Izquierda)
* **Placa Medieval Translúcida:** Fondo de pergamino beige traslúcido con borde dorado fino y marco de madera café oscuro.
* **Información Integrada:** Muestra el rostro del jugador, nombre en blanco cálido, rol actual (con su color representativo), vidas del trono (`TRONO: <vidas>`), puntos compartidos (`TEAM VIDAS: <puntos>` con base/máximo de 1000) y el tiempo restante de juego.
* **Tiempo en Vivo:** Formateado como `TIEMPO: H:MM:SS`, actualizado localmente cada segundo en el cliente y sincronizado periódicamente con el servidor.
* **Exención de Operadores (OP):** Los administradores con permiso OP (nivel 2+) nunca son expulsados cuando su tiempo llega a 00:00:00.

### 2. Brújula Deslizante & Coordenadas (Superior Central)
* **Brújula RPG Horizontal:** Barra deslizante que resalta las direcciones cardinales (`N`, `NE`, `E`, `SE`, `S`, `SO`, `O`, `NO`) en dorado brillante conforme el jugador gira.
* **Coordenadas Compactas:** Muestra `X: ... Y: ... Z: ...` en una sola línea compacta e integrada justo debajo de la brújula.

### 3. Sistema de Notificaciones Notificativas (Esquina Superior Derecha)
* **Tarjetas Compactas:** Mueve alertas de misiones completadas/aceptadas, subidas de nivel, recompensas, habilidades desbloqueadas y asedios a la esquina superior derecha.
* **Desaparición Automática:** Muestra avisos limpios durante 3.5 segundos sin sobrecargar el Chat ni la Action Bar.
* **Estado de Asedio Activo:** Al colocar una Carga de Asalto, muestra la base atacada, vida actual del trono, nombre del equipo atacante y el tiempo restante de detonación.

### 4. Diálogos de NPCs Físicamente Separados & 1x1
* **Archivos Individuales:** Los diálogos se almacenan en archivos independientes bajo `world/mundo_de_tronos2/npc_dialogues/`:
  - `diosa_maria.json`
  - `sacerdote.json`
  - `samuel.json`
  - `heraldo.json`
  - `monje_destino.json`
  - `custodio_trono.json`
* **Interfaces Aisladas:** Cada NPC abre su propia pantalla sin pestañas ni elementos mezclados de otros personajes.
* **Conversaciones Progresivas 1x1:** Los diálogos avanzan paso a paso (ej. mediante botones de *Continuar*) para simular una narrativa fluida.

### 5. Editor de NPCs en Tiempo Real para OPs
* **Acceso:** `SHIFT + Clic Derecho` sobre cualquier NPC teniendo permisos de OP (Nivel 2+).
* **Edición Árbol Completo:** Permite modificar en tiempo real el nombre, skin, nodos de diálogo, respuestas, destinos (`nextNode`) y acciones/disparadores (`action`). Todos los cambios se guardan y persisten de inmediato en el servidor.

### 6. Misión de Samuel & Armadura Inicial
* **Requisitos:** Requiere completar la misión de entregar 1 PAN a Samuel **Y** tener **Nivel 10** para poder reclamar la armadura inicial del rol.
* **Equipamiento Libre:** La armadura entregada por Samuel está configurada con `AuthorizedRole = "any"`, permitiendo que cualquier jugador la equipe sin restricciones de clase.

### 7. Yunque de Reparaciones Exclusivo
* **Interfaz Dedicada:** Se eliminó la forja general para evitar interfaces mezcladas.
* **Reparación de Armaduras:** Acepta únicamente armaduras de inicio o de rol de clase dañadas.
* **Costo Fijo:** Cada reparación requiere exactamente **3 Lingotes de Hierro** (`Iron Ingot`) verificados y consumidos directamente por el servidor.

### 8. Árbol de Habilidades Pantalla Completa (Tecla M)
* **Diseño Extendido:** Ocupa casi toda la pantalla con un fondo de pergamino translúcido medieval.
* **Nodos Interactivos:** Diseñados como botones cuadrados transparentes con bordes dorados, verdes o marrones según su estado (Disponible, Desbloqueado o Bloqueado).
* **Conexiones & Tooltips:** Renderiza líneas rústicas conectando nodos prerrequisito y tooltips descriptivos al pasar el cursor.

### 9. Perfil & Carnet de Identidad (Tecla K)
* **Ligado al UUID:** Se eliminó el objeto físico del Carnet para prevenir pérdidas, duplicaciones o robos. El Carnet se consulta desde `Tecla K -> Mi Perfil -> VER CARNET`.

### 10. Trono, Protección 150x150 & Límites Visibles
* **Protección de Base:** Al colocar un Trono, se genera automáticamente una zona protegida de **150x150 bloques** (75 bloques de radio desde el Trono) donde solo los miembros del equipo pueden construir o interactuar.
* **El Custodio del Trono:** NPC que entrega el ítem oficial del Trono (`mundodetronos2:throne_item = true`) y un libro guía únicamente al **Líder** del equipo.
* **Comando de Límites:** `/tronos limites` activa o desactiva la visualización de bordes mágicos con partículas en el cliente.

### 11. Dimensión de Roles & Monje del Destino
* **Primer Ingreso:** El jugador aparece en la dimensión mística donde el **Monje del Destino** actúa como tutor. Tras dialogar y elegir rol ante la Diosa María, realiza el minijuego QTE y la cinemática de pestañeo.
* **Ingresos Posteriores:** Si el jugador ya posee un rol, ingresa a la dimensión normalmente sin cinemáticas ni pantallas obligatorias.

### 12. Progreso por Mobs Configurable
* **XP por Kills:** Derrotar mobs otorga XP de progreso para subir de nivel de rol.
* **Configuración:** Editable en `config/mundo_de_tronos2.json` (`enableMobKillXp`, `defaultMobKillXp`, `bossMobKillXp`).

---

## ⌨ Comandos Disponibles

### Comandos de Jugador
* `/tronos crear <nombre>` — Crea un nuevo equipo/reino.
* `/tronos buscar` — Abre el catálogo de reinos creados.
* `/tronos misreinos` — Gestiona tu equipo actual, miembros y estado del trono.
* `/tronos invitaciones` — Consulta invitaciones pendientes.
* `/tronos aceptar <id>` / `/tronos rechazar <id>` — Acepta o rechaza invitaciones.
* `/tronos salir` — Abandona tu equipo actual.
* `/tronos info` — Muestra el estado textual de tu base.
* `/tronos limites` — Alterna la visualización de los bordes translúcidos de tu base 150x150.

### Comandos de Administrador (OP Nivel 2+)
* `/tronos npc crear <tipo> <nombre>` — Genera un NPC en la posición exacta del jugador (`diosa_maria`, `sacerdote`, `samuel`, `heraldo`, `monje_destino`, `custodio_trono`).
* `/tronos npc eliminar` — Elimina el NPC más cercano al jugador.
* `/tronos herrero resetkit <jugador>` — Restablece las misiones de NPC y el kit inicial del jugador objetivo para realizar pruebas.
* `/tronos rol asignar <jugador> <rol>` — Asigna un rol directamente a un jugador.
* `/tronos rol quitar <jugador>` — Quita el rol asignado a un jugador.
* `/tronos evento on/off` — Activa o desactiva la fase global de asedio.
* `/tronos setvidas <reino> <cantidad>` — Ajusta las vidas del trono de un equipo.
* `/tronos reload` / `/tronos save` — Recarga la configuración o fuerza el guardado en disco.
* `/tronos debug` — Desglosa un informe completo del estado del mod, HUD, NPCs, diálogos, roles, tronos y asedios.

---

## 🛠 Instrucciones de Prueba y Verificación

1. **Prueba de Teclas K y M:**
   - Selecciona un rol en la dimensión de roles o asigna uno con `/tronos rol asignar <jugador> guerrero`.
   - Presiona `K` para abrir el perfil y consulta `VER CARNET`.
   - Presiona `M` para abrir el Árbol de Habilidades en pantalla completa.
   - Cambia de dimensión o muere y reaparece; confirma que `K` y `M` siguen respondiendo de inmediato sin necesidad de reconectarte.

2. **Prueba de Misión de Samuel y Armadura:**
   - Usa `/tronos herrero resetkit <jugador>` para reiniciar misiones.
   - Habla con Samuel, acepta su misión y entrégale 1 PAN.
   - Si tu nivel es menor a 10, intenta reclamar la armadura; confirma que el sistema te notifica `Necesitas nivel 10`.
   - Sube de nivel a 10 (ganando XP con misiones o matando mobs) y reclama la armadura; confirma que se entrega y puedes equiparla libremente.

3. **Prueba del Yunque de Reparaciones:**
   - Equipa una pieza de armadura inicial o de rol dañada.
   - Interactúa con el Yunque de Roles teniendo al menos 3 Lingotes de Hierro en tu inventario.
   - Selecciona la pieza dañada y presiona `REPARAR`; confirma que la durabilidad se restaura al 100% y se consumen exactamente 3 lingotes de hierro.

4. **Prueba del Custodio y Colocación del Trono:**
   - Como líder de equipo, habla con el Custodio del Trono e interactúa para recibir el ítem del Trono.
   - Colócalo en el suelo; confirma que se crea una zona protegida de 150x150 bloques.
   - Ejecuta `/tronos limites` para verificar la aparición de las partículas border en los límites del territorio.

5. **Prueba de Asedio y Carga de Asalto:**
   - Activa el evento global con `/tronos evento on`.
   - Coloca una Carga de Asalto cerca de un trono enemigo; confirma que la tarjeta de asedio se despliega en la esquina superior derecha mostrando la base, vida del trono, nombre del atacante y cuenta regresiva.
   - Confirma que la textura de la TNT utiliza los recursos vanilla sin bloques morados/negros.

---

## 📦 Compilación

Para compilar el proyecto:
```bash
./gradlew clean build
```

El archivo `.jar` compilado se generará en:
```text
build/libs/mundodetronos2-1.0.0.jar
```
