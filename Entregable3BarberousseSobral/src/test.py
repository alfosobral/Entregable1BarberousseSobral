from lark import Lark
from dsl_externo import LARK_GRAMMAR
Lark(LARK_GRAMMAR, parser="lalr")
print("gramática ok")