from lark import Lark, Transformer, v_args, Tree
from lark.exceptions import LarkError
from modelos import Partido
from dsl_interno import DSLValidationError

"""
Aqui definimos la gramática del Lark

-- ¿Qué es Lark? --

Lark es una librería de Python para crear parsers (analizadores sintácticos).
Sirve para leer lenguajes personalizados, o sea un DSL externo, y convertirlos 
en estructuras Python que tu programa pueda usar.

Podemos verlo como un traductor que convierte:

    Gol: BAR, 34', 10, 9  ---> ("GOL", ("BAR", 34, 10, 9)) 

-- Conceptos clave --

1) Gramática: le dice a Lark qué formas de texto son válidas (reglas del idioma).
2) Tokens: los átomos del lenguaje (palabras, números, signos, etc.).
3) Transformer: convierte el árbol que genera Lark en objetos Python concretos.

-- Gramática básica -- 

gol: "Gol:" COD3 "," MINUTO "," NUM ("," NUM)? EOL

Esta regla le dice a Lark: 
"Para reconocer un gol buscá: 
    - la palabra literal "Gol:"
    - un código de 3 letras COD3
    - una coma ","
    - un número de minuto MINUTO
    - otra coma ","
    - un número de jugador NUM
    - opcionalmente otra coma y otro número (el asistente)
    - un salto de línea EOL (End Of Line)

-- Tokens --

Los Tokens se definen con expresiones regulares (regex)

    - COD3: /[A-Za-z]{3}/     3 letras seguidas
    - NUM: DIGIT+             uno o más dígitos
    - MINUTO: DIGIT+ "'"?     uno o más dígitos seguidos de un "'" opcional (por ejemplo 90')

-- Estructura jerárquica --

La regla más alta es siempre start, que define la estructura general del archivo.

    start: EOL* partido ( (EOL+ | sep) partido )* EOL*

Esto significa:
“El archivo está compuesto por uno o más partidos, separados por saltos de línea o separadores”.

Y cada partido tiene su mini lenguaje:

    partido: fecha local visitante form_local form_visit ...

-- ¿Cómo Lark procesa el texto? --

Cuando se ejecuta la linea

    tree = parser.parse(texto)

Lark lee el texto línea por línea buscando coincidencias con las reglas de la gramática y
genera un árbol sintáctico.

-- Transformer --

El transformer convierte el árbol sintáctico en objetos. El árbol es puro texto, entonces 
Lark necesita que le digas qué hacer con él.

La clase ToModel hereda de Transformer y define métodos con el mismo nombre que las reglas 
de la gramática.

    gol: "Gol:" COD3 "," ... ---> def gol(self, _kw, cod, ...)

Lark ve que la regla gol fue reconocida y llama a ToModel.gol(...) con los valores que extrajo.

El método los convierte en Python nativo (int, str, etc.) y devuelve una tupla que se guarda en 
el árbol transformado.

-- Resultado final --

El transformer recorre el árbol completo y ejecuta todos esos métodos, transformando cada parte 
del texto en algo útil:

    - Fechas a string o datetime
    - Códigos de equipos a string
    - Goles a tuplas o listas
    - Partidos → objetos Partido

Así, model.partidos queda como una lista de objetos Partido listos para usar.

-- Resumen -- 

texto → Lark.parse() → árbol → Transformer → objetos Python →  programa
"""

LARK_GRAMMAR = r"""
start: EOL* partido ( (EOL+ | sep) partido )* EOL*

partido: fecha local visitante form_local form_visit titulares_local titulares_visit banco_local? banco_visit? evento*

fecha: "Fecha:" FECHA EOL
local: "Equipo Local:" COD3 EOL
visitante: "Equipo Visitante:" COD3 EOL
form_local: "Formación Local:" FORM EOL
form_visit: "Formación Visitante:" FORM EOL
titulares_local: "Titulares Local:" LISTA_NUM EOL
titulares_visit: "Titulares Visitante:" LISTA_NUM EOL
banco_local: "Banco Local:" LISTA_NUM EOL
banco_visit: "Banco Visitante:" LISTA_NUM EOL

evento: gol
      | tarjeta
      | cambio
      | sep

gol: "Gol:" COD3 "," MINUTO "," NUM ("," NUM)? EOL
tarjeta: "Tarjeta:" COD3 "," MINUTO "," NUM "," COLOR EOL
cambio: "Cambio:" COD3 "," MINUTO "," NUM "," NUM EOL

sep: "---" EOL
   | "##" EOL

COLOR: /(Amarilla|Roja)/
LISTA_NUM: NUM ("," NUM)*
FORM: DIGIT+ ("-" DIGIT+)+
MINUTO: DIGIT+ "'"?
COD3: /[A-Za-z]{3}/
FECHA: /\d{4}-\d{2}-\d{2}|(\d{1,2}\/\d{1,2}\/\d{4})/
NUM: DIGIT+

EOL: /(\r?\n)+/

%import common.DIGIT
%ignore /[ \t]+/          // ignorar espacios/tabs
%ignore /[ \t]*#[^\n]*/   // comentarios hasta fin de línea
"""


def _parse_lista(nums):
    s = str(nums)
    return [int(x.strip()) for x in s.split(",") if x.strip()]

@v_args(inline=True)
class ToModel(Transformer):
    def __init__(self):
        super().__init__()
        self.partidos = []

    def partido(self, fecha, local, visitante, fL, fV, tL, tV, *tail):
        bL = bV = None
        eventos = []

        idx = 0
        if idx < len(tail) and isinstance(tail[idx], str):
            bL = tail[idx]; idx += 1
        if idx < len(tail) and isinstance(tail[idx], str):
            bV = tail[idx]; idx += 1
        eventos = tail[idx:]

        p = Partido(str(fecha), str(local).upper(), str(visitante).upper())
        p.formacion_local = str(fL)
        p.formacion_visitante = str(fV)
        p.titulares_local = _parse_lista(tL)
        p.titulares_visitante = _parse_lista(tV)
        if bL: p.banco_local = _parse_lista(bL)
        if bV: p.banco_visitante = _parse_lista(bV)

        for e in eventos:
            if not e:
                continue
            if isinstance(e, Tree):
                continue
            etype, data = e
            if etype == "GOL":
                p.goles.append(data)
            elif etype == "TARJ":
                p.tarjetas.append(data)
            elif etype == "CAMB":
                p.cambios.append(data)

        self.partidos.append(p)
        return p


    def evento(self, item):
        return item

    def fecha(self, fecha, *_eol):            return str(fecha)
    def local(self, cod, *_eol):              return str(cod)
    def visitante(self, cod, *_eol):          return str(cod)
    def form_local(self, form, *_eol):        return str(form)
    def form_visit(self, form, *_eol):        return str(form)
    def titulares_local(self, lista, *_eol):  return str(lista)
    def titulares_visit(self, lista, *_eol):  return str(lista)
    def banco_local(self, lista, *_eol):      return str(lista)
    def banco_visit(self, lista, *_eol):      return str(lista)
    
    def gol(self, cod, minuto, autor, *rest):
        asist = None
        if rest and isinstance(rest[0], int):
            asist = int(rest[0])
        m = int(str(minuto).rstrip("'"))
        return ("GOL", (str(cod).upper(), m, int(autor), asist))
        
    def tarjeta(self, cod, minuto, nro, color, *_eol):
        m = int(str(minuto).rstrip("'"))
        return ("TARJ", (str(cod).upper(), m, int(nro), str(color)))

    def cambio(self, cod, minuto, sale, entra, *_eol):
        m = int(str(minuto).rstrip("'"))
        return ("CAMB", (str(cod).upper(), m, int(sale), int(entra)))
    
    def sep(self, *args):
        return None

    def LISTA_NUM(self, t): return t.value
    def FORM(self, t): return t.value
    def FECHA(self, t): return t.value
    def MINUTO(self, t): return t.value
    def COD3(self, t): return t.value
    def COLOR(self, t): return t.value
    def NUM(self, t): return int(t.value)

class DSLPartidos:
    def __init__(self, liga):
        self.liga = liga
        self._parser = Lark(LARK_GRAMMAR, parser="lalr", maybe_placeholders=False)

    def cargar_texto(self, texto):
        try:
            tree = self._parser.parse(texto)
            tx = ToModel()
            tx.transform(tree)
            partidos = tx.partidos
        except LarkError as e:
            raise DSLValidationError(f"Error de sintaxis en DSL externo: {e}") from e

        for p in partidos:
            self._validar_partido(p)
            self._registrar_partido(p)
        return partidos
    
    def _registrar_partido(self, p):
        liga = self.liga

        if not hasattr(liga, "partidos"):
            liga.partidos = []
        if not hasattr(liga, "tabla_puntos"):
            liga.tabla_puntos = {}
        if not hasattr(liga, "goleadores"):
            liga.goleadores = {}

        clave = (p.fecha, p.cod_local, p.cod_visitante)
        for ex in liga.partidos:
            if (ex.fecha, ex.cod_local, ex.cod_visitante) == clave:
                return

        for cod in (p.cod_local, p.cod_visitante):
            if cod not in liga.tabla_puntos:
                liga.tabla_puntos[cod] = {"pts": 0, "gf": 0, "gc": 0}

        for (cod_eq, minuto, autor, asist) in p.goles:
            key = (cod_eq, autor)
            liga.goleadores[key] = liga.goleadores.get(key, 0) + 1

        try:
            gl, gv = p.marcador()
        except AttributeError:
            gl = sum(1 for g in p.goles if g[0] == p.cod_local)
            gv = sum(1 for g in p.goles if g[0] == p.cod_visitante)

        liga.tabla_puntos[p.cod_local]["gf"] += gl
        liga.tabla_puntos[p.cod_local]["gc"] += gv
        liga.tabla_puntos[p.cod_visitante]["gf"] += gv
        liga.tabla_puntos[p.cod_visitante]["gc"] += gl

        if gl == gv:
            liga.tabla_puntos[p.cod_local]["pts"] += 1
            liga.tabla_puntos[p.cod_visitante]["pts"] += 1
        elif gl > gv:
            liga.tabla_puntos[p.cod_local]["pts"] += 3
        else:
            liga.tabla_puntos[p.cod_visitante]["pts"] += 3

        liga.partidos.append(p)

    def _validar_partido(self, p: Partido):
        eqL = self.liga.get_equipo(p.cod_local)
        eqV = self.liga.get_equipo(p.cod_visitante)
        if eqL is None or eqV is None:
            raise DSLValidationError(f"Equipo inexistente (local={p.cod_local}, visitante={p.cod_visitante}). "
                                     f"Cargá equipos en el DSL interno primero.")

        if len(p.titulares_local) != 11:
            raise DSLValidationError(f"{p.cod_local}: deben ser 11 titulares (recibidos {len(p.titulares_local)}).")
        if len(p.titulares_visitante) != 11:
            raise DSLValidationError(f"{p.cod_visitante}: deben ser 11 titulares (recibidos {len(p.titulares_visitante)}).")

        for nro in p.titulares_local:
            if nro not in eqL.jugadores:
                raise DSLValidationError(f"{p.cod_local}: no existe jugador #{nro} en el plantel.")
        for nro in p.titulares_visitante:
            if nro not in eqV.jugadores:
                raise DSLValidationError(f"{p.cod_visitante}: no existe jugador #{nro} en el plantel.")

        for (cod, m, autor, asist) in p.goles:
            eq = self.liga.get_equipo(cod)
            if eq is None: 
                raise DSLValidationError(f"Equipo en gol no existe: {cod}.")
            if autor not in eq.jugadores:
                raise DSLValidationError(f"Gol invalido: {cod} jugador autor #{autor} no existe.")
            if asist is not None and asist not in eq.jugadores:
                raise DSLValidationError(f"Asistencia invalida: {cod} jugador asistente #{asist} no existe.")

        for (cod, m, nro, color) in p.tarjetas:
            eq = self.liga.get_equipo(cod)
            if eq is None or nro not in eq.jugadores:
                raise DSLValidationError(f"Tarjeta invalida: {cod} jugador #{nro} no existe.")

        for (cod, m, sale, entra) in p.cambios:
            eq = self.liga.get_equipo(cod)
            if eq is None or sale not in eq.jugadores or entra not in eq.jugadores:
                raise DSLValidationError(f"Cambio invalido: {cod} sale #{sale}, entra #{entra}.")

        liga = self.liga
        if not hasattr(liga, "partidos"):
            liga.partidos = []
        if not hasattr(liga, "tabla_puntos"):
            liga.tabla_puntos = {}
        if not hasattr(liga, "goleadores"):
            liga.goleadores = {} 

        liga.partidos.append(p)
        l, v = p.cod_local, p.cod_visitante
        for cod in (l, v):
            liga.tabla_puntos.setdefault(cod, {"pts":0, "gf":0, "gc":0})

        for (cod, m, autor, asist) in p.goles:
            key = (cod, autor)
            liga.goleadores[key] = liga.goleadores.get(key, 0) + 1

        gl, gv = p.marcador()
        liga.tabla_puntos[l]["gf"] += gl
        liga.tabla_puntos[l]["gc"] += gv
        liga.tabla_puntos[v]["gf"] += gv
        liga.tabla_puntos[v]["gc"] += gl

        ganador = p.ganador()
        if ganador is None:
            liga.tabla_puntos[l]["pts"] += 1
            liga.tabla_puntos[v]["pts"] += 1
        elif ganador == l:
            liga.tabla_puntos[l]["pts"] += 3
        else:
            liga.tabla_puntos[v]["pts"] += 3


    def tabla_posiciones(self):
        t = []
        for cod, row in getattr(self.liga, "tabla_puntos", {}).items():
            dif = row["gf"] - row["gc"]
            t.append((cod, row["pts"], row["gf"], row["gc"], dif))
        t.sort(key=lambda x: (x[1], x[4], x[2]), reverse=True)
        return t

    def tabla_goleadores(self):
        arr = [ (cod, nro, goles) for (cod, nro), goles in getattr(self.liga, "goleadores", {}).items() ]
        arr.sort(key=lambda x: x[2], reverse=True)
        return arr

    def listar_resultados(self):
        res = []
        for p in getattr(self.liga, "partidos", []):
            gl, gv = p.marcador()
            res.append((p.fecha, p.cod_local, p.cod_visitante, gl, gv))
        return res
