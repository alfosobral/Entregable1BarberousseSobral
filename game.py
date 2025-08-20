import random 

def throw_dice():
    return random.randint(1,6)

def create_new_payer(name):
    return {"name": name, "pos": 0, "coins":2}

