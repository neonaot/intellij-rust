import argparse
import json
import os
import re
from urllib import request

import subprocess

from common import env
from updater import UpdaterBase

"""
This script serves to download actual lints from rustc and clippy.
You need to have `rustc` and `git` available in $PATH for it to work.
"""

DIR = os.path.dirname(os.path.abspath(__name__))
RUSTC_LINTS_PATH = "src/main/kotlin/org/rust/lang/core/completion/lint/RustcLints.kt"
CLIPPY_LINTS_PATH = "src/main/kotlin/org/rust/lang/core/completion/lint/ClippyLints.kt"


class LintParsingMode:
    Start = 0
    ParsingLints = 1
    LintsParsed = 2
    ParsingGroups = 3


def get_rustc_lints():
    result = subprocess.run(["rustc", "-W", "help"],
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            stdin=subprocess.DEVNULL,
                            check=True)
    output = result.stdout.decode()

    def normalize(name):
        return name.replace("-", "_")

    lint_regex = re.compile(r"^([a-z0-9]+-)*[a-z0-9]+$")
    parsing = LintParsingMode.Start
    lints = []
    for line in output.splitlines():
        line_parts = [part.strip() for part in line.strip().split()]
        if len(line_parts) == 0:
            if parsing == LintParsingMode.ParsingLints:
                parsing = LintParsingMode.LintsParsed
            continue
        if "----" in line_parts[0]:
            if parsing == LintParsingMode.Start:
                parsing = LintParsingMode.ParsingLints
            elif parsing == LintParsingMode.LintsParsed:
                parsing = LintParsingMode.ParsingGroups
            continue
        if parsing == LintParsingMode.ParsingLints and lint_regex.match(line_parts[0]):
            lints.append((normalize(line_parts[0]), False))
        if parsing == LintParsingMode.ParsingGroups and lint_regex.match(line_parts[0]):
            lints.append((normalize(line_parts[0]), True))
    return lints


def get_clippy_lints():
    data = request.urlopen("https://rust-lang.github.io/rust-clippy/master/lints.json")
    clippy_lints = json.loads(data.read())

    groups = set()
    lints = []
    for lint in clippy_lints:
        lints.append((lint["id"], False))
        groups.add(lint["group"])

    merged_lints = lints + [(group, True) for group in groups]
    if ("all", True) not in merged_lints:
        merged_lints.append(("all", True))

    return merged_lints


class LintsUpdater(UpdaterBase):

    def _update_locally(self):
        hardcoded_part_rustc = """/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint

val RUSTC_LINTS: List<Lint> = listOf(
"""  # dont forget ')'

        lints = get_rustc_lints()
        lints.sort()

        text = ""
        for i in lints:
            text += "    Lint(\"{}\", {}),\n".format(i[0], str(i[1]).lower())

        with open(RUSTC_LINTS_PATH, "w") as f:
            f.write(hardcoded_part_rustc + text + ")")

        hardcoded_part_clippy = """/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint

val CLIPPY_LINTS: List<Lint> = listOf("""
        lints = get_clippy_lints()
        lints.sort()

        text = ""
        for i in lints:
            text += "    Lint(\"{}\", {}),\n".format(i[0], str(i[1]).lower())

        with open(CLIPPY_LINTS_PATH, "w") as f:
            f.write(hardcoded_part_clippy + text + ")")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True, help="github token")
    args = parser.parse_args()

    repo = env("GITHUB_REPOSITORY")

    updater = LintsUpdater(repo, args.token, branch_name="rustc-lints", message="Update lints", assignee="neonaot")
    updater.update()


if __name__ == '__main__':
    main()
