import json
from modelos import Liga, Equipo

class DSLInvalidState(Exception):
    pass

class DSLValidationError(Exception):
    pass

class Liga:
    def __init__(self):
        self.equipos_por_codigo = {}

    def agregar_equipo(self, nombre: str, codigo: str):
        codigo = codigo.upper() if len(codigo) <= 3 else codigo[:3].upper()
        if codigo in self.equipos_por_codigo:
            raise DSLValidationError(
                f"Ya existe un equipo con código '{codigo}'."
            )
        equipo = Equipo(nombre, codigo)
        self.equipos_por_codigo[codigo] = equipo
        return equipo

    def get_equipo(self, codigo: str):
        return self.equipos_por_codigo.get(codigo.upper())

    def to_dict(self):
        return {cod: eq.to_dict() for cod, eq in self.equipos_por_codigo.items()}


class DSL:
    def __init__(self):
        self._liga = Liga()
        self._equipo_actual = None
        self._cerrado = False

    def _requerir_equipo_actual(self):
        if self._equipo_actual is None:
            raise DSLInvalidState("No hay equipo actual. Hay que llamar primero a .equipo(nombre, codigo).")

    def _requerir_no_cerrado(self):
        if self._cerrado:
            raise DSLInvalidState("El DSL ya fue cerrado con .build(). Hay que crear una instancia nueva.")

    def equipo(self, nombre: str, codigo: str | None = None):
        self._requerir_no_cerrado()
        codigo = codigo[:3].upper() if codigo else nombre[:3].upper()
        equipo = self._liga.get_equipo(codigo)
        if equipo is None:
            equipo = self._liga.agregar_equipo(nombre, codigo)
        else:
            if nombre and equipo.nombre != nombre:
                equipo.nombre = nombre
        self._equipo_actual = equipo
        return self

    def jugador(self, numero: int, nombre: str):
        self._requerir_no_cerrado()
        self._requerir_equipo_actual()
        self._equipo_actual.agregar_jugador(numero, nombre)
        return self

    def fin_equipo(self):
        self._requerir_no_cerrado()
        self._equipo_actual = None
        return self

    def build(self):
        self._requerir_no_cerrado()
        self._cerrado = True
        self._equipo_actual = None
        return self._liga

if __name__ == "__main__":
    liga = (
        DSL()
        .equipo("Barcelona", "BA")
            .jugador(1, "Ter Stegen")
            .jugador(9, "Lewandowski")
            .jugador(10, "Lamine Yamal")
        .equipo("Real Madrid", "RMA")
            .jugador(1, "Courtois")
            .jugador(7, "Vinícius Júnior")
            .jugador(11, "Rodrygo")
        .build()
    )

    # Imprimimos para verificar que todo esta ok

    print(json.dumps(liga.to_dict(), ensure_ascii=False, indent=2))
