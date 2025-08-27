# Entregable 1: Programación Funcional - Barberousse Luciana, Sobral Alfonso

### Indicaciones de ejecución

Para ejecutar el programa debe correrse el archivo game.py. Al iniciar, se solicitará seleccionar el modo de juego:

Modo simulación (s): El juego se desarrolla de manera automática. El usuario observa en la terminal la evolución de los jugadores en el tablero, las tiradas de dado y los efectos de bonus o penalizaciones. La partida finaliza cuando un jugador alcanza la casilla 30 o se queda sin monedas. En caso de que un jugador esté en la casilla 29 y obtenga un número mayor a 1, igualmente gana sin necesidad de un “rebote”. Una vez terminada la partida, se pregunta si se desea jugar nuevamente (s/n). En caso afirmativo, se reinicia el proceso de selección de modo.

Modo interactivo (i): Antes de comenzar se solicitan los nombres de los jugadores, que no pueden estar vacíos. El turno inicia con el primer nombre ingresado.
Cada tirada de dado se realiza presionando Enter, y el programa informa el valor obtenido junto con los posibles bonus o penalizaciones en monedas y movimiento.
En cada turno también se muestra el tablero actualizado con la posición de los jugadores. La partida finaliza cuando alguno alcanza la casilla 30 o se queda sin monedas.

### Decisiones tomadas para implementar el juego

__State__

Para representar el estado del juego decidimos utilizar un diccionario _state_, que encapsula toda la información necesaria para el desarrollo de la partida. Este enfoque permite trabajar de manera más clara y ordenada, ya que el estado no se modifica directamente, sino que en cada paso se genera un nuevo estado a partir del anterior (inmutabilidad).

El state es un diccionario (Dict[str, object]) con la siguiente estructura:
- "players": tupla con los nombres de los dos jugadores. Ejemplo: ("Ana", "Pepe").
- "positions": diccionario que indica la casilla actual de cada jugador. Ejemplo: {"Ana": 5, "Pepe": 3}.
- "coins": diccionario con la cantidad de monedas de cada jugador. Ejemplo: {"Ana": 2, "Pepe": 1}.
- "move": diccionario que asigna a ciertas casillas del tablero un bonus o penalización de movimiento. Ejemplo: {7: +2, 12: "reset"}.
- "econ": diccionario que asigna a ciertas casillas del tablero un bonus o penalización de monedas. Ejemplo: {8: +1, 15: -1}.

De esta manera, todo el flujo del juego puede modelarse a partir de este estado central, aplicando funciones puras que lo transforman paso a paso.

__Tablero visual__

A la hora de realizar el tablero por consola hicimos uso de IA para poder llegar al resultado que queríamos pero siempre aprendiendo de lo que nos devuelve, por lo tanto decidimos dejarle una sección de este documento para describir lo que hace luego de haberla comprendido.

Se conforma por 4 funciones:
- def visible_len(s: str) -> int: Calcula la longitud visible de un string, eliminando los códigos de color ANSI (que no ocupan espacio real en la terminal). Esto es para alinear bien en consola, pues no importan los códigos de color.
  
- def pad_center_visible(s: str, width: int) -> str: Centra un string en un espacio de ancho fijo, considerando solo los caracteres visibles (sin contar los códigos de color). Basicamente chequea cuanto ocupa el string que se debe imprimir en terminal, calcula la diferencia entre el espacio isponible y lo que ocupa para manejar dicho espacio libre  así que quede todo alineado.
  
- def format_cell(i: int, state, width: int = 5) -> str: Construye el texto que representa una celda del tablero:
  - Muestra el número de casilla (ej: 01, 02, ...). Decidimos utilizar dos digitos para que cada numero de casilla ocupe el mismo ancho y quede visualmente mas estetico.
  - Si hay un jugador en esa casilla, muestra su inicial y la colorea. El jugador 1 aparece en rojo y el jugador 2 en azul.
  - Si la casilla tiene bonus/penalización de salto, agrega un símbolo > magenta.
  - Si la casilla tiene bonus/penalización de monedas, agrega un símbolo $ amarillo.
  - Si hay ambos bonus se agregan ambos a la casilla con la misma logica.

- def render_board(state, boxes=BOXES, per_row=6, width=5): Dibuja el tablero completo en la terminal:
  - Llama a format_cell para cada casilla del tablero.
  - Agrupa las casillas en filas de 6 (por defecto). Este parametro puede cambiarse dentro del codigo. 
  - Imprime cada fila.

### Algunas funciones

- _def generate_random_dice() -> Generator[int, None, None]_

Esta función se encarga de generar tiradas infinitas de un dado de 6 caras. Utiliza random.randint(1,6) para producir un número aleatorio y yield para ir entregando cada valor. Gracias al uso de generadores, el estado se mantiene entre llamadas, lo que permite simular una secuencia ilimitada de lanzamientos.

- _def generate_special_boxes() -> Tuple[Dict[int, object], Dict[int, int]]_

Su función es crear casillas especiales en el tablero. Devuelve dos diccionarios:
    - jumps: contiene casillas que generan efectos de movimiento (avanzar, retroceder o reiniciar a 0).
    - econ: contiene casillas que otorgan o quitan monedas.
  
Para garantizar aleatoriedad, primero desordenan dos copias de las posiciones del tablero con random.shuffle, y luego asigna allí los bonus y penalizaciones. De esta forma, cada partida tiene una distribución distinta de casillas especiales, y permite que dos bonificaciones de distinto tipo aparezcane en la misma celda.

- _def pure_step(state: State, player: str, throw: int) -> State_

Esta función define cómo evoluciona el estado del juego tras un turno. Llamamos _chain_ (cadena) a un evento poco usual en el juego, en el que un jugador tiene una muy buena racha de suerte y le aplican varias bonificaciones de corrido. Para esto, el codigo se encarga de calcular de forma recusriva todas las bonificaciones de la tirada aplicando una funcion recursiva. Primero, calcula el "paso cero" utilizando la posicion inicial del jugador y el valor de la tirada del dado. En base a esto, chequea las eventuales bonificaciones en cada "casilla intermedia" llamando a la funcion recursiva _apply_chain_. Esta funcion continua calculando bonificaciones hasta que el jugador cae en una casilla vacia o el juego termina.

El nombre pure_step resalta que es una función pura: no tiene efectos secundarios y siempre devuelve el mismo resultado dados los mismos parámetros.

- _def endgame(state: State) -> bool:  Aqui chequeamos para cada jugador si llego al final, y si tiene monedas suficientes_

Se utiliza para detectar si la partida terminó. 
- Revisa si algún jugador llegó o superó la casilla final (BOXES) con al menos una moneda.
- Si ambos jugadores llegan al final, gana el que conserve más monedas (o se declara empate si tienen la misma cantidad).
- También contempla el caso en que un jugador se quede sin monedas en el medio de la partida, otorgando la victoria automática al otro.

Es una funcion booleana (devuelve True si la partida terminó, o False en caso contrario).

- _def simul(state: State, dice: Generator[int, None, None]) -> Generator[State, None, None]_

Implementa el modo simulador del juego.

- Alterna los turnos de los jugadores, lanzando el dado con el generador dice.
- En cada turno actualiza el estado con pure_step y lo devuelve mediante yield.
- El bucle continúa hasta que endgame detecta que hay un ganador.

Esto permite recorrer la partida como una secuencia de estados, ideal para animaciones o ejecuciones automáticas.

### Conclusiones de lo aprendido en el proyecto 
- (Analice muy brevemente en qué casos fue posible y en qué casos no, mantener la inmutabilidad)

