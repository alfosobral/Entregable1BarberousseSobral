import os

from dsl_interno import DSL
from dsl_externo import DSLPartidos, DSLValidationError

def cargar_equipos_iniciales():
    return (
        DSL()
        .equipo("Barcelona", "BAR")
            .jugador(1, "Ter Stegen")
            .jugador(3, "Koundé")
            .jugador(5, "Íñigo")
            .jugador(7, "Ferran Torres")
            .jugador(9, "Lewandowski")
            .jugador(10, "Lamine Yamal")
            .jugador(11, "Raphinha")
            .jugador(12, "Cancelo")
            .jugador(15, "Christensen")
            .jugador(17, "Gündogan")
            .jugador(20, "Félix")
            .jugador(13, "Iñaki Peña")
            .jugador(14, "Joao Cancelo")
            .jugador(16, "Fermín")
            .jugador(18, "Pedri")
        .equipo("Real Madrid", "RMA")
            .jugador(1, "Courtois")
            .jugador(2, "Carvajal")
            .jugador(3, "Militão")
            .jugador(4, "Alaba")
            .jugador(5, "Bellingham")
            .jugador(6, "Camavinga")
            .jugador(7, "Vinícius")
            .jugador(8, "Kroos")
            .jugador(9, "Rodrygo")
            .jugador(10, "Modrić")
            .jugador(11, "Valverde")
            .jugador(12, "Lunin")
            .jugador(13, "Kepa")
            .jugador(14, "Mendy")
            .jugador(15, "Tchouaméni")
        .equipo("Liverpool", "LIV")
            .jugador(1, "Alisson")
            .jugador(2, "Trent A-A")
            .jugador(3, "Robertson")
            .jugador(4, "Van Dijk")
            .jugador(5, "Konaté")
            .jugador(6, "Szoboszlai")
            .jugador(7, "Luis Díaz")
            .jugador(8, "Mac Allister")
            .jugador(9, "Darwin Núñez")
            .jugador(10, "Salah")
            .jugador(11, "Gakpo")
        .equipo("Manchester City", "MCI")
            .jugador(1, "Ederson")
            .jugador(2, "Walker")
            .jugador(3, "Akanji")
            .jugador(4, "Dias")
            .jugador(5, "Gvardiol")
            .jugador(6, "Rodri")
            .jugador(7, "Foden")
            .jugador(8, "Kovacic")
            .jugador(9, "Haaland")
            .jugador(10, "Bernardo")
            .jugador(11, "Doku")
        .build()
    )

def print_tabla_posiciones(tabla, liga):
    print("\nTabla de posiciones")
    print("===================")
    print(f"{'Equipo':<6}  {'Pts':>3}  {'GF':>3}  {'GC':>3}  {'Dif':>3}  Nombre")
    for cod, pts, gf, gc, dif in tabla:
        nombre = liga.get_equipo(cod).nombre if liga.get_equipo(cod) else "?"
        print(f"{cod:<6}  {pts:>3}  {gf:>3}  {gc:>3}  {dif:>3}  {nombre}")

def print_goleadores(gols, liga):
    print("\nTabla de goleadores")
    print("===================")
    print(f"{'Equipo':<6}  {'#':>3}  {'Goles':>5}  Nombre")
    for cod, nro, g in gols:
        eq = liga.get_equipo(cod)
        nom = eq.jugadores[nro].nombre if eq and nro in eq.jugadores else "?"
        print(f"{cod:<6}  {nro:>3}  {g:>5}  {nom}")

def print_resultados(lista):
    print("\nResultados")
    print("==========")
    for fecha, l, v, gl, gv in lista:
        print(f"{fecha}: {l} {gl}-{gv} {v}")

def main():
    liga = cargar_equipos_iniciales()
    dslp = DSLPartidos(liga)

    while True:
        print("\nMenú")
        print("1) Cargar partidos desde archivo")
        print("2) Ver tabla de posiciones")
        print("3) Ver tabla de goleadores")
        print("4) Listar resultados")
        print("0) Salir")
        op = input("> ").strip()

        if op == "1":
            ruta = input("Ruta del archivo (ej: data/partidos.txt): ").strip()
            if not os.path.exists(ruta):
                print("Archivo no encontrado.")
                continue
            with open(ruta, "r", encoding="utf-8") as f:
                texto = f.read()
            try:
                partidos = dslp.cargar_texto(texto)
                print(f"✓ Se cargaron {len(partidos)} partido(s).")
            except DSLValidationError as e:
                print("ERROR:", e)
        elif op == "2":
            print_tabla_posiciones(dslp.tabla_posiciones(), liga)
        elif op == "3":
            print_goleadores(dslp.tabla_goleadores(), liga)
        elif op == "4":
            print_resultados(dslp.listar_resultados())
        elif op == "0":
            break
        else:
            print("Opción inválida.")

if __name__ == "__main__":
    main()
