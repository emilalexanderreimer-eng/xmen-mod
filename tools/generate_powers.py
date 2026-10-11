"""Regenerates Powers.java and PowerCategory.java from tools/powers.psv."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/java/com/emil/xmen/power"


def java_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


rows = [line.rstrip("\n").split("|") for line in open(ROOT / "tools/powers.psv", encoding="utf-8")
        if line.strip() and not line.startswith("#")]
categories = {}
ids = set()
adds = []
for section, name, level, kind, description in rows:
    match = re.match(r"^(.*?)(?: \((.*)\))?$", name)
    display, based_on = match.group(1), match.group(2) or ""
    power_id = re.sub(r"[^a-z0-9]+", "_", display.lower()).strip("_")
    if power_id in ids:
        raise SystemExit("duplicate power id: " + power_id)
    ids.add(power_id)
    category = re.sub(r"[^A-Z0-9]+", "_", section.upper().replace("&", "AND")).strip("_")
    categories[category] = section
    adds.append(f"        add({java_str(power_id)}, {java_str(display)}, {java_str(based_on)}, "
                f"PowerLevel.{level.upper()}, PowerCategory.{category}, PowerType.{kind.upper()},\n"
                f"                {java_str(description)});")

template = (OUT / "Powers.java").read_text(encoding="utf-8")
start = template.index("    static {\n") + len("    static {\n")
end = template.index("\n    }\n", start)
(OUT / "Powers.java").write_text(template[:start] + "\n".join(adds) + template[end:], encoding="utf-8")

enum_values = ",\n".join(f'    {c}("{s}")' for c, s in categories.items())
(OUT / "PowerCategory.java").write_text(f"""package com.emil.xmen.power;

public enum PowerCategory {{
{enum_values};

    private final String displayName;

    PowerCategory(String displayName) {{
        this.displayName = displayName;
    }}

    public String displayName() {{
        return displayName;
    }}
}}
""", encoding="utf-8")
print(f"{len(ids)} powers written")
