from typing import Dict, Tuple, Generator
import functools
import random
from colorama import Fore, Style, init

init(autoreset=True) # para que funcione en Windows/Linux

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

def color_text(text: str, color=None) -> str:
    if color:
        return f"{color}{text}{Style.RESET_ALL}"
    else:
        return text

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
    players = state["players"]
    positions = state["positions"]
    coins = state["coins"]
    winners = [p for p in players if positions[p] >= BOXES and coins[p] >= 1]
    if len(winners) == 1:
        print(f"Ganó {winners[0]}")
        return True
    if len(winners) == 2:
        if coins[players[0]] > coins[players[1]]:
            print(f"Ganó {players[0]} por más monedas")
        elif coins[players[1]] > coins[players[0]]:
            print(f"Ganó {players[1]} por más monedas")
        else:
            print("Empate: ambos tienen la misma cantidad de monedas")
        return True
    for p in players:
        if coins[p] == 0:
            print(f"Ganó {players[1] if p == players[0] else players[0]} porque {p} se quedó sin monedas")
            return True
    return False

# -_-_-_-_-_-_-_-_-_-
# Tablero visual
# -_-_-_-_-_-_-_-_-_-

def board(state: State) -> None:
    def board_cell(i: int) -> str:
        cell = str(i)
        for p, pos in state["positions"].items():
            if pos == i:
                cell = color_text(f"[{p[0]}]", Fore.CYAN)
        if i in state["move"]:
            cell += color_text("*", Fore.MAGENTA)
        if i in state["econ"]:
            cell += color_text("$", Fore.YELLOW)
        return cell.rjust(5) 
    board_im = "".join(map(board_cell, range(1, BOXES + 1)))
    print(board_im)


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
    while True:
        while True:
            mode = input("Seleccione el mode de juego: Simulacion (s) | Iterativo (i) --> ")
            if mode in ("s", "i"):
                break
            print("El modo de juego no es correcto. Por favor ingrese un modo valido...")
        jumps, econ = generate_special_boxes()
        if mode == "i":
            name1 = input("Nombre del jugador 1: ")
            name2 = input("Nombre de jugador 2: ")
            players = (name1, name2)
        elif mode == "s":
            players = ("Jugador1", "Jugador2")
        else: 
            print("El modo dejuego no es correcto")

        initial_state = {
            "players": players,
            "positions": {players[0]: 0, players[1]: 0},
            "coins": {players[0]: START_COINS, players[1]: START_COINS},
            "move": jumps,
            "econ": econ,
        }

        dice = generate_random_dice()
        if mode == "i":
            state = initial_state
            while not endgame(state):
                for player in state["players"]:
                    input(f"Turno de {player}. Presiona Enter para tirar el dado...")
                    throw = next(dice)
                    print(f"{player} sacó un {throw}")
                    old_pos = state["positions"][player]
                    new_pos = min(BOXES, old_pos + throw)
                    bonus_jump = state["move"].get(new_pos, 0)
                    bonus_econ = state["econ"].get(new_pos, 0)
                    print(f"Casilla: {new_pos}")
                    if bonus_jump:
                        print(f"Bonus de salto: {bonus_jump}")
                    if bonus_econ:
                        print(f"Bonus de monedas: {bonus_econ}")
                    state = pure_step(state, player, throw)
                    board(state)
                    print(f"Estado del tablero: {state}")
                    if endgame(state):
                        print("FIN DEL JUEGO")
                        break    
        elif mode == "s": 
            for st in simul(initial_state, dice):
                board(st)
                if endgame(st):
                    print("FIN DEL JUEGO")
                    break
        
        again = input("¿Quieres volver a jugar? (s/n): ")
        if again == "n":
            print("Gracias por jugar!!!")
            break
    

        
