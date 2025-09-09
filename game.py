from typing import Dict, Tuple, Generator
import functools, os, re, time, random
from colorama import Fore, Style, init
from datetime import datetime

init(autoreset=True) 

# -_-_-_-_-_-_-_-_-_-
# Funciones auxiliares
# -_-_-_-_-_-_-_-_-_-

def clear_console():
    if os.name == "nt":
        os.system("cls")
    else:
        os.system("clear")

# -_-_-_-_-_-_-_-_-_-
# Log
# -_-_-_-_-_-_-_-_-_-    

LOG_ENABLED = True

def log_call(fn):
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        result = fn(*args, **kwargs)
        if LOG_ENABLED:
            arg_str = ""
            if len(args) >= 2 and isinstance(args[1], str):
                arg_str = f"jugador={args[1]}"
            if len(args) >= 3 and isinstance(args[2], int):
                arg_str += f", dado={args[2]}"
            if isinstance(result, dict):
                res_str = ""
                if "positions" in result and "coins" in result and arg_str:
                    jugador = args[1]
                    res_str = f"pos={result['positions'][jugador]}, monedas={result['coins'][jugador]}"
                else:
                    res_str = "{...}"
            else:
                res_str = repr(result)
            #print(f"[LOG] {fn.__name__}({arg_str}) -> {res_str}")
        return result
    return wrapper

def print_turn_header(player: str, turn_no: int):
    title = f"TURNO {turn_no} — {player}"
    bar = "═" * len(title)
    # Encabezado con una línea en blanco antes y después
    print(f"\n{bar}\n{title}\n{bar}\n")


def create_log(players):
    date = datetime.now().strftime("%Y%m%d-%H%M%S")
    log_name = "vs".join(players)
    log = f"{log_name}_{date}.txt"
    return open(log, "w", encoding="utf-8")

# -_-_-_-_-_-_-_-_-_-
# Variables globales y estructuras
# -_-_-_-_-_-_-_-_-_-

BOXES = 30
START_COINS = 2
COLORS = {
    "J1": Fore.RED,
    "J2": Fore.BLUE,
    "mov": Fore.MAGENTA,
    "econ": Fore.YELLOW,
}
RESET = Style.RESET_ALL
ANSI_RE = re.compile(r'\x1b\[[0-9;]*m')

State = Dict[str, object]

# -_-_-_-_-_-_-_-_-_-
# Generadores
# -_-_-_-_-_-_-_-_-_-

def generate_random_dice() -> Generator[int, None, None]:
    rand = random.Random()
    while (True):
        yield rand.randint(1,6)

# -_-_-_-_-_-_-_-_-_-
# Inicializacion de la partida
# -_-_-_-_-_-_-_-_-_-

def generate_special_boxes() -> Tuple[Dict[int, object], Dict[int, int]]:
    rand = random.Random()
    boxes1 = list(range(1, BOXES))
    boxes2 = list(range(1, BOXES))
    rand.shuffle(boxes1)
    rand.shuffle(boxes2)

    jumps = {
        boxes1[0] : +2,
        boxes1[1] : +1,
        boxes1[2] : +3,
        boxes1[3] : "reset",
        boxes1[4] : -5
    }

    econ = {
        boxes2[0] : +2,
        boxes2[1] : +1,
        boxes2[2] : +1,
        boxes2[3] : -1,
        boxes2[4] : -1
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
    pos0   = state["positions"][player]
    coins0 = state["coins"][player]
    pos1 = min(BOXES, pos0 + throw)
    move = state["move"]
    econ = state["econ"]
    def apply_chain(pos: int, coins: int, calls: int = 0, cache=None) -> tuple[int, int]:
        if cache is None:
            cache = set()
        bonus_econ = econ.get(pos, 0)
        bonus_jump = move.get(pos, 0)
        coins2 = compute_econ(coins, bonus_econ) if bonus_econ != 0 else coins
        pos2 = min(BOXES, compute_next_jump(pos, bonus_jump) if bonus_jump != 0 else pos)
        if bonus_jump == 0:
            return pos, coins2
        if pos2 == pos and coins2 == coins:
            return pos, coins
        key = (pos2, coins2)
        if key in cache or calls > 100:
            cache.add(key)
            return pos2, coins2
        return apply_chain(pos2, coins2, calls + 1, cache)
    final_pos, final_coins = apply_chain(pos1, coins0)
    return {**state,
        "positions": {**state["positions"], player: final_pos},
        "coins":     {**state["coins"],     player: final_coins},
    }

# -_-_-_-_-_-_-_-_-_-
# Endgame
# -_-_-_-_-_-_-_-_-_-

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
# Turno: describir, aplicar y mostrar
# -_-_-_-_-_-_-_-_-_-

def describe_and_apply_turn(state: State, player: str, throw: int, log_file) -> State:
    ms1 = f"Turno de {player}"
    print(ms1)
    if log_file:
        log_file.write(ms1 + "\n")
        log_file.flush()
    
    ms2 = f"{player} sacó un {throw}"
    print(ms2)
    if log_file:
        log_file.write(ms2 + "\n")
        log_file.flush()

    old_pos   = state["positions"][player]
    old_coins = state["coins"][player]

    # Calcula el estado REAL (incluye encadenamientos y economía) — SOLO una pasada
    new_state = pure_step(state, player, throw)

    new_pos   = new_state["positions"][player]
    new_coins = new_state["coins"][player]

    expected = min(BOXES, old_pos + throw)
    if new_pos != expected:
        ms3 = "Hubo premio/castigo de movimiento aplicado"
        print(ms3)
        if log_file:
            log_file.write(ms3 + "\n")
            log_file.flush()

    delta_coins = new_coins - old_coins
    if   delta_coins > 0: print(f"Ganó monedas: +{delta_coins}")
    elif delta_coins < 0: print(f"Perdió monedas: {delta_coins}")

    if new_coins != old_coins:
        ms4 = f"Movimiento: {old_pos} -> {new_pos} --- Monedas: {old_coins} -> {new_coins}"
        print(ms4)
        if log_file:
            log_file.write(ms4 + "\n")
            log_file.flush()
    else:
        ms4 = f"Movimiento: {old_pos} -> {new_pos} --- Monedas: {old_coins}"
        print(ms4)
        if log_file:
            log_file.write(ms4 + "\n")
            log_file.flush()

    board = render_board(new_state)
    print(board)
    if log_file:
        log_file.write("\n")
        log_file.write("\n")
        log_file.flush()

    return new_state


# -_-_-_-_-_-_-_-_-_-
# Tablero visual
# -_-_-_-_-_-_-_-_-_-

def visible_len(s: str) -> int:
    return len(ANSI_RE.sub('', s))

def pad_center_visible(s: str, width: int) -> str:
    v = visible_len(s)
    if v >= width:
        return s
    total_pad = width - v
    left = total_pad // 2
    right = total_pad - left
    return ' ' * left + s + ' ' * right

def format_cell(i: int, state, width: int) -> str:
    base = f"{i:02}"

    players_here = [p for p, pos in state["positions"].items() if pos == i]
    if players_here:
        names = list(state["positions"].keys())
        who = players_here[0]
        idx = names.index(who)  # 0 -> J1, 1 -> J2
        color = COLORS["J1"] if idx == 0 else COLORS["J2"]
        base = f"[{who[0].upper()}]"
        base = f"{color}{base}{RESET}"

    suffix = ""
    if i in state["move"]:
        suffix += f"{COLORS['mov']}>{RESET}"
    if i in state["econ"]:
        suffix += f"{COLORS['econ']}${RESET}"

    cell_text = base + suffix

    extra_visible = visible_len(cell_text) - width
    if extra_visible > 0:
        if suffix:
            if i in state["econ"]:
                cell_text = base + f"{COLORS['mov']}*{RESET}" if i in state["move"] else base
            elif i in state["move"]:
                cell_text = base
    return pad_center_visible(cell_text, width)

def render_board(state, boxes=BOXES, per_row=10, width=5):
    cells = list(map(lambda i: format_cell(i, state, width), range(1, boxes + 1)))
    lines = [" ".join(cells[r:r+per_row]) for r in range(0, boxes, per_row)]
    return "\n".join(lines)

# -_-_-_-_-_-_-_-_-_-
# Simulador (generador automatico de estados)
# -_-_-_-_-_-_-_-_-_-

def simul(state: State, dice: Generator[int, None, None]) -> Generator[State, None, None]:
    players = state["players"]
    turn = 0
    while not endgame(state):
        player = players[turn % 2]
        throw = next(dice)
        yield state, player, throw
        state = pure_step(state, player, throw)
        turn += 1

# -_-_-_-_-_-_-_-_-_-
# Main
# -_-_-_-_-_-_-_-_-_-

if __name__ == "__main__":
    while True:
        while True: 
            clear_console()
            mode = input("Seleccione el mode de juego: Simulacion (s) | Interactivo (i) --> ").lower()
            print("\n")
            if mode in ("s", "i"):
                break
            print("El modo de juego no es correcto. Por favor ingrese un modo valido...")
        jumps, econ = generate_special_boxes()
        if mode == "i":
            name1 = input("Nombre del jugador 1: ")
            while name1 == "":
                name1 = input("Nombre del jugador 1: ")
            name2 = input("Nombre de jugador 2: ")
            while name2 == "" or name2 == name1:
                name2 = input("Nombre de jugador 2: ")
            players = (name1, name2)
            log_file = create_log(players)
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
            game_over = False
            turn_no = 1 
            while not game_over:
                for player in state["players"]:
                    #clear_console()
                    print_turn_header(player, turn_no)  
                    turn_no += 1
                    input(f"Turno de {player}. Presiona Enter para tirar el dado...")
                    print("\n")
                    throw = next(dice)
                    state = describe_and_apply_turn(state, player, throw, log_file)
                    if endgame(state):
                        final_ms = "FIN DEL JUEGO"
                        log_file.write(final_ms)
                        log_file.flush()
                        print()
                        print(final_ms)
                        input()
                        game_over = True
                        break
                    print()
                    # Mostrar jugadores con monedas usando filter
                    jugadores_con_monedas = list(filter(lambda p: state["coins"][p] > 0, state["players"]))
                    print("Jugadores con monedas:", jugadores_con_monedas)
                    # Mostrar total de monedas usando reduce
                    from functools import reduce
                    total_monedas = reduce(lambda acc, p: acc + state["coins"][p], state["players"], 0)
                    print("Total de monedas en juego:", total_monedas)
                    input(f"Presione enter para continuar...")

        elif mode == "s":
            players = ("Jugador1", "Jugador2")
            log_file = create_log(players)
            turn_no = 1
            for st_before, player, throw in simul(initial_state, dice):
                print_turn_header(player, turn_no) 
                turn_no += 1
                state = describe_and_apply_turn(st_before, player, throw, log_file)
                print("\n")
                # Mostrar jugadores con monedas usando filter
                jugadores_con_monedas = list(filter(lambda p: state["coins"][p] > 0, state["players"]))
                print("Jugadores con monedas:", jugadores_con_monedas)
                # Mostrar total de monedas usando reduce
                from functools import reduce
                total_monedas = reduce(lambda acc, p: acc + state["coins"][p], state["players"], 0)
                print("Total de monedas en juego:", total_monedas)
                log_file.flush()
                time.sleep(3)
                #clear_console()
                if endgame(state):
                    final_ms = "FIN DEL JUEGO"
                    log_file.write(final_ms)
                    log_file.flush()
                    print("FIN DEL JUEGO")
                    break
            log_file.close()

        again = input("¿Quieres volver a jugar? (s/n): ")
        while again != "s" and again != "n":
            print("Debe ingresar una opción válida.")
            again = input("¿Quieres volver a jugar? (s/n): ")
        if again == "n":
            print("Gracias por jugar!!!")
            break
