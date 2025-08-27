from typing import Dict, Tuple, Generator
import functools, os, re, time, datetime, random
from colorama import Fore, Style, init

init(autoreset=True) # para que funcione en Windows/Linux

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

def log_call(fn):
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        result = fn(*args, **kwargs)
        #print(f"[LOG] {fn.__name__}{args} -> {result}")
        return result
    return wrapper
        
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
        be = econ.get(pos, 0)
        bm = move.get(pos, 0)
        coins2 = compute_econ(coins, be) if be != 0 else coins
        pos2 = compute_next_jump(pos, bm) if bm != 0 else pos
        pos2 = min(BOXES, pos2)
        if pos2 == pos and coins2 == coins:
            return pos, coins
        key = (pos2, coins2)
        if key in cache or calls > 100:
            return pos2, coins2
        cache.add(key)
        return apply_chain(pos2, coins2, calls + 1, cache)
    final_pos, final_coins = apply_chain(pos1, coins0)
    return {
        **state,
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

def format_cell(i: int, state, width: int = 5) -> str:
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

def render_board(state, boxes=BOXES, per_row=6, width=5):
    cells = [format_cell(i, state, width) for i in range(1, boxes + 1)]
    lines = [" ".join(cells[r:r+per_row]) for r in range(0, boxes, per_row)]
    print("\n".join(lines))

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
            clear_console()
            mode = input("Seleccione el mode de juego: Simulacion (s) | Interactivo (i) --> ").lower()
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
                    clear_console()
                    input(f"Turno de {player}. Presiona Enter para tirar el dado...")
                    throw = next(dice)
                    print(f"{player} sacó un {throw}")
                    old_pos = state["positions"][player]
                    new_pos = min(BOXES, old_pos + throw)
                    old_econ = state["coins"][player]
                    bonus_jump = state["move"].get(new_pos, 0) if state["move"].get(new_pos, 0) else 0
                    bonus_econ = state["econ"].get(new_pos, 0)
                    if isinstance(bonus_jump, str):
                        print(Fore.RED + f"{player} VUELVE A 0" + Style.RESET_ALL)
                    elif bonus_jump > 0:
                        print(f"Bonus de salto: +{bonus_jump}")
                    elif bonus_jump < 0:
                        print(f"Penalización de salto: {bonus_jump}")
                    if bonus_econ > 0:
                        print(f"Bonus de monedas: +{bonus_econ}")
                    elif bonus_econ < 0:
                        print(f"Penalización economía: {bonus_econ}")
                    new_econ = compute_econ(old_econ, bonus_econ)
                    new_pos_after_bonus = compute_next_jump(new_pos, bonus_jump)
                    print(f"Movimiento: {old_pos} -> {new_pos_after_bonus} --- Monedas: {new_econ}")
                    state = pure_step(state, player, throw)
                    render_board(state)
                    if endgame(state):
                        print("FIN DEL JUEGO")
                        input()
                        break    
                    print()
                    input(f"Presione enter para continuar...")
        elif mode == "s": 
            for st in simul(initial_state, dice):
                render_board(st)
                time.sleep(3)
                clear_console()
                if endgame(st):
                    print("FIN DEL JUEGO")
                    break
        
        again = input("¿Quieres volver a jugar? (s/n): ")
        if again == "n":
            print("Gracias por jugar!!!")
            break