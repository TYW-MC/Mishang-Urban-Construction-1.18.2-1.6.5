#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""删除 1.18.2 中不存在的木材变体（CHERRY / MANGROVE / BAMBOO）。

策略：
  1) 删除「public static final ... 」且名称/初始化器涉及变体的字段声明（连同其前置注解行）。
  2) 删除纯单元素列表条目行（形如 `X.Y,` 或 `X.Y`）。
  3) 对内联的 List.of(...) / ImmutableSet.of(...) 逐元素剔除变体元素。
  4) 其余无法安全处理的行原样保留，交由编译错误逐个处理。
"""
import io
import os
import re

ROOT = r"F:\mishanguc-1.18.2-fabric\src\main\java"
TARGETS = [
    r"pers\solid\mishang\uc\blocks\ColoredBlocks.java",
    r"pers\solid\mishang\uc\blocks\HandrailBlocks.java",
    r"pers\solid\mishang\uc\blocks\HungSignBlocks.java",
    r"pers\solid\mishang\uc\blocks\StandingSignBlocks.java",
    r"pers\solid\mishang\uc\blocks\WallSignBlocks.java",
    r"pers\solid\mishang\uc\data\MishangucRecipeProvider.java",
    r"pers\solid\mishang\uc\Mishanguc.java",
    r"pers\solid\mishang\uc\MishangUtils.java",
]

# 变体标识符（如 CHERRY_WOOD_HUNG_SIGN、STRIPPED_CHERRY_WOOD_...）或 Blocks.CHERRY_*
VAR_RE = re.compile(r"(?:CHERRY|MANGROVE|BAMBOO)[A-Z0-9_]*")
BLOCK_RE = re.compile(r"Blocks\.(?:CHERRY|MANGROVE|BAMBOO)[A-Z0-9_]*")
# 形如 `X.Y.z = a.b = c.d = ...;`（变体纹理赋值语句）
TEXTURE_ASSIGN_RE = re.compile(r"^\s*(?:[A-Za-z_][A-Za-z0-9_]*\.)?[A-Z0-9_]+(?:\.[A-Za-z_][A-Za-z0-9_]*)+(\s*=|\s*\.)")
# 形如 `new Identifier("block/cherry_log")` 等包含变体路径的语句
PATH_HINT_RE = re.compile(r'"(?:block|mishanguc:block)/(?:stripped_)?(?:cherry|mangrove|bamboo)[a-z0-9_]*"')
ANNOT_RE = re.compile(r"^\s*@")
SINGLE_ELEM_COMMA = re.compile(r"^\s*(?:[A-Za-z_][A-Za-z0-9_]*\.)?[A-Z0-9_]+\s*,\s*$")
SINGLE_ELEM_LAST = re.compile(r"^\s*(?:[A-Za-z_][A-Za-z0-9_]*\.)?[A-Z0-9_]+\s*$")
OF_RE = re.compile(r"\b(?:List|ImmutableSet|Set)\.of\(([^()]*)\)")

report = {}

for rel in TARGETS:
    path = os.path.join(ROOT, rel)
    if not os.path.exists(path):
        print("MISSING:", path)
        continue
    with io.open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    out = []
    i = 0
    removed = 0
    while i < len(lines):
        line = lines[i]

        # --- 1) 注解块 + 字段声明 ---
        if ANNOT_RE.match(line):
            j = i
            annots = []
            while j < len(lines) and ANNOT_RE.match(lines[j]):
                annots.append(lines[j])
                j += 1
            if j < len(lines):
                decl = lines[j]
                if re.search(r"(?:public|private|protected)\s+static\s+final", decl) and (
                    VAR_RE.search(decl) or BLOCK_RE.search(decl)
                ):
                    k = j
                    while k < len(lines) and ";" not in lines[k]:
                        k += 1
                    removed += (k - i + 1)
                    i = k + 1
                    continue
            out.extend(annots)
            i = j
            continue

        # 无注解的字段声明
        if re.search(r"(?:public|private|protected)\s+static\s+final", line) and (
            VAR_RE.search(line) or BLOCK_RE.search(line)
        ):
            k = i
            while k < len(lines) and ";" not in lines[k]:
                k += 1
            removed += (k - i + 1)
            i = k + 1
            continue

        # --- 2) 纯单元素列表条目 ---
        if VAR_RE.search(line) and SINGLE_ELEM_COMMA.match(line):
            removed += 1
            i += 1
            continue
        if VAR_RE.search(line) and SINGLE_ELEM_LAST.match(line):
            while out and out[-1].strip() == "":
                out.pop()
            if out:
                out[-1] = re.sub(r",\s*$", "", out[-1].rstrip("\n")) + "\n"
            removed += 1
            i += 1
            continue

        # --- 3a) 整行仅设置变体纹理的赋值语句 ---
        if "=" in line and "new Identifier(" in line and PATH_HINT_RE.search(line):
            # 该行所有 Identifier 路径都必须是变体纹理路径
            paths = re.findall(r'new Identifier\("([^"]*)"\)', line)
            if paths and all(PATH_HINT_RE.fullmatch('"%s"' % p) for p in paths):
                removed += 1
                i += 1
                continue

        # --- 3) 内联 of(...) 剔除元素 ---
        if VAR_RE.search(line) or BLOCK_RE.search(line):
            def strip_of(m):
                inner = m.group(1)
                parts = [p for p in inner.split(",") if not VAR_RE.search(p) and not BLOCK_RE.search(p)]
                return m.group(0)[: m.group(0).index("(") + 1] + ",".join(parts) + ")"
            new = OF_RE.sub(strip_of, line)
            if new != line:
                out.append(new)
                removed += 1
                i += 1
                continue
            # 其余情况：保留原行（后续编译错误再逐个处理）
            out.append(line)
            i += 1
            continue

        out.append(line)
        i += 1

    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    report[rel] = removed

for k, v in report.items():
    print(f"{v:4d}  {k}")
print("TOTAL removed lines:", sum(report.values()))
