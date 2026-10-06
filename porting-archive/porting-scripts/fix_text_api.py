#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""修复 1.20.1 -> 1.18.2 的 Text API 差异：
   X.getContent() instanceof LiteralText      -> X instanceof LiteralText
   ((LiteralText) X.getContent()).string()    -> ((LiteralText) X).getRawString()
   someLiteralText.string()                   -> someLiteralText.getRawString()
"""
import io
import re
import sys

FILES = [
    r"F:\mishanguc-1.18.2-fabric\src\main\java\pers\solid\mishang\uc\item\TextCopyToolItem.java",
    r"F:\mishanguc-1.18.2-fabric\src\main\java\pers\solid\mishang\uc\text\TextContext.java",
    r"F:\mishanguc-1.18.2-fabric\src\main\java\pers\solid\mishang\uc\screen\TextFieldListWidget.java",
]

# ((LiteralText) <expr>.getContent()).string()
RE_CAST = re.compile(r"\(\(\s*LiteralText\s*\)\s*(.+?)\.getContent\(\)\)\.string\(\)")
# <expr>.getContent() instanceof [final ]LiteralText
RE_INST = re.compile(r"([A-Za-z_][A-Za-z0-9_.\[\]()]*?)\.getContent\(\)\s+instanceof\s+(final\s+)?LiteralText")
# <ident>.string()   (仅当不是常见非文本类型时)
RE_STRLIT = re.compile(r"\b([A-Za-z_][A-Za-z0-9_]*)\.string\(\)")

for path in FILES:
    with io.open(path, "r", encoding="utf-8") as f:
        src = f.read()
    orig = src
    src = RE_CAST.sub(lambda m: "((LiteralText) %s).getRawString()" % m.group(1).strip(), src)
    src = RE_INST.sub(lambda m: "%s instanceof %sLiteralText" % (m.group(1), m.group(2) or ""), src)
    src = RE_STRLIT.sub(lambda m: "%s.getRawString()" % m.group(1), src)
    if src != orig:
        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(src)
        print("PATCHED", path)
    else:
        print("no change", path)
