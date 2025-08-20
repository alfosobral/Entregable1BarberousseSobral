from typing import Dict, Tuple, Generator
import functools
import random

# -_-_-_-_-_-_-_-_-_-
# Funciones auxiliares
# -_-_-_-_-_-_-_-_-_-


def compose(*funcs):
    return functools.reduce(lambda f, g: lambda x: f(g(x)), funcs)

def log_call(fn):
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        result = fn(*args, **kwargs)
        print(f"[LOG] {fn.__name__}{args} -> {result}")
        return result
    return wrapper

# -_-_-_-_-_-_-_-_-_-
# Modelo de datos
# -_-_-_-_-_-_-_-_-_-

# Definimos variables globales (inmutables) como el tamanio del tablero y 
# la cantidad de monedas iniciales.

BOXES = 30
START_COINS = 2

State = Dict[str, object]

# -_-_-_-_-_-_-_-_-_-
# Generadores
# -_-_-_-_-_-_-_-_-_-

# Esta funcion se usa para generar tiradas infinitas del dado. Usa yield para
# recordar su estado entre llamadas, y random para generar un numero aleatroio
# entre 1 y 6.

def generate_random_dice() -> Generator[int, None, None]:
    rand = random.Random()
    while (True):
        yield rand.randint(1,6)

# -_-_-_-_-_-_-_-_-_-
# Inicializacion de la partida
# -_-_-_-_-_-_-_-_-_-

# Aqui debemos generar posiciones aleatorias dentro del tablero para los bonus
# y las penalizaciones

def generate_special_boxes() -> Tuple[Dict[int, object], Dict[int, int]]:
    rand = random.Random()
    boxes = list(range(1, BOXES))
    rand.shuffle(boxes)

    # Bonus de saltos

    jumps = {
        boxes[0] : +2,
        boxes[1] : +1,
        boxes[2] : +3,
        boxes[3] : "reset",
        boxes[4] : -5
    }

    # Bonus de monedas

    econ = {
        boxes[5] : +2,
        boxes[6] : +1,
        boxes[7] : +1,
        boxes[8] : -1,
        boxes[9] : -1
     }
    
    return jumps, econ

# -_-_-_-_-_-_-_-_-_-
# Logica del juego
# -_-_-_-_-_-_-_-_-_-

@log_call
def compute_next_jump(position: int, bonus) -> int:
    if bonus == "reset":
        return 0
    else: 
        return max(0, position + bonus)
    
@log_call
def compute_econ(coins: int, bonus: int) -> int:
    return max(0, coins + bonus)

@log_call
def pure_step(state: State, player: str, throw: int) -> State:
    position = state["positions"][player]
    new_position = min(BOXES, position + throw)

    # Aplicamos los bonus de salto

    bonus_jump = state["move"].get(new_position, 0)
    new_position = compute_next_jump(new_position, bonus_jump)

    # Aplicamos los bonus de monedas (econ)

    coins = state["coins"][player]
    bonus_econ = state["econ"].get(new_position, 0)
    new_coins = compute_econ(coins, bonus_econ)

    # Ahora devolvemos un nuevo estado (inmutabilidad)

    return {
        **state,
        "positions": {**state["positions"], player: new_position},
        "coins" : {**state["coins"], player: new_coins} 
    }

# -_-_-_-_-_-_-_-_-_-
# Endgame
# -_-_-_-_-_-_-_-_-_-

# Aqui chequeamos para cada jugador si llego al final, y si tiene monedas 
# suficientes

def endgame(state: State) -> bool:
    for p in state["players"]:
        if state["positions"][p] >= BOXES and state["coins"][p] >= 1:
            return True
        if state["coins"][p] == 0:
            return True 
    return False

# -_-_-_-_-_-_-_-_-_-
# Simulador (generador automatico de estados)
# -_-_-_-_-_-_-_-_-_-

def simul(state: State, dice: Generator[int, None, None]) -> Generator[State, None, None]:
    players = state["players"]
    turn = 0
    while not endgame(state):
        player = players[turn % 2]
        throw = next(dice)
        state = pure_step(state, player, throw)
        yield state
        turn += 1

# -_-_-_-_-_-_-_-_-_-
# Main
# -_-_-_-_-_-_-_-_-_-

if __name__ == "__main__":
    jumps, econ = generate_special_boxes()
    initial_state = {
        "players": ("P1", "P2"),
        "positions": {"P1": 0, "P2": 0},
        "coins": {"P1": START_COINS, "P2": START_COINS},
        "move": jumps,
        "econ": econ,
    }

    dice = generate_random_dice()
    for st in simul(initial_state, dice):
        print(st)
        if endgame(st):
            print("JUEGO TERMINADO")
            break
