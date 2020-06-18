import ast

modules = []
for node in ast.walk(ast.parse(program_string)):
    if isinstance(node, ast.ImportFrom):
        modules.append(node.module)
    elif isinstance(node, ast.Import):
        modules.append(node.names[0].name)