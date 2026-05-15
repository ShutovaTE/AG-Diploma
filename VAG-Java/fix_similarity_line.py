from pathlib import Path
p = Path(__file__).resolve().parent / "src" / "main" / "webapp" / "WEB-INF" / "views" / "artwork" / "details.html"
text = p.read_text("utf-8")
old = """Совпадение: <span th:text=\"${similar.matchPercentage} + '%'\"></span>"""
new = """Совпадение: <span th:text=\"${similar.matchPercentage} + '%'\"></span>"""
print('found', old in text)
text = text.replace(old, new)
p.write_text(text, "utf-8")
print('done')
