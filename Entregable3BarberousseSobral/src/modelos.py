class Jugador:
    def __init__(self, numero, nombre):
        self.numero = int(numero)
        self.nombre = str(nombre)
        self.goles = 0
        self.tarjetas = []

    def to_dict(self):
        return {"numero": self.numero, "nombre": self.nombre, "goles": self.goles}


class Equipo:
    def __init__(self, nombre, codigo):
        self.nombre = str(nombre)
        self.codigo = str(codigo).upper()
        self.jugadores = {}

    def agregar_jugador(self, numero, nombre):
        nro = int(numero)
        if nro <= 0 or nro > 99:
            raise ValueError("Número de camiseta debe ser 1..99")
        if nro in self.jugadores:
            raise ValueError(f"Duplicado de camiseta #{nro} en {self.codigo}")
        self.jugadores[nro] = Jugador(nro, nombre)

    def to_dict(self):
        return {
            "nombre": self.nombre,
            "codigo": self.codigo,
            "jugadores": [j.to_dict() for j in self.jugadores.values()],
        }


class Liga:
    def __init__(self):
        self.equipos_por_codigo = {}
        self.partidos = []
        self.tabla_puntos = {}
        self.goleadores = {}

    def agregar_equipo(self, nombre, codigo):
        cod = str(codigo).upper()
        if not (len(cod) == 3 and cod.isalpha()):
            raise ValueError("Código de equipo debe ser 3 letras")
        if cod in self.equipos_por_codigo:
            raise ValueError(f"Equipo duplicado: {cod}")
        eq = Equipo(nombre, cod)
        self.equipos_por_codigo[cod] = eq
        return eq

    def get_equipo(self, codigo):
        return self.equipos_por_codigo.get(str(codigo).upper())


class Partido:
    def __init__(self, fecha, cod_local, cod_visitante):
        self.fecha = str(fecha)
        self.cod_local = str(cod_local).upper()
        self.cod_visitante = str(cod_visitante).upper()
        self.formacion_local = None
        self.formacion_visitante = None
        self.titulares_local = []
        self.titulares_visitante = []
        self.banco_local = []
        self.banco_visitante = []
        self.goles = []
        self.tarjetas = []
        self.cambios = []

    def marcador(self):
        gl = sum(1 for g in self.goles if g[0] == self.cod_local)
        gv = sum(1 for g in self.goles if g[0] == self.cod_visitante)
        return gl, gv

    def ganador(self):
        gl, gv = self.marcador()
        if gl > gv: return self.cod_local
        if gv > gl: return self.cod_visitante
        return None
