import argparse
import re

from common import execute_command, env
from updater import UpdaterBase

CHECK_WORKFLOW_PATH = ".github/workflows/check.yml"
RUSTC_STABLE_VERSION_RE = re.compile(r"[0-9][.][0-9]{2}[.][0-9]")
WORKFLOW_RUSTC_STABLE_VERSION_RE = re.compile(r"RUST_STABLE_VERSION: ([0-9][.][0-9]{2}[.][0-9])")


class StableUpdater(UpdaterBase):
    def _update_locally(self):
        execute_command("rustup", "default", "stable")
        output = execute_command("rustc", "-V")
        version = RUSTC_STABLE_VERSION_RE.search(output).group(0)

        with open(CHECK_WORKFLOW_PATH) as f:
            workflow_text = f.read()

        result = re.search(WORKFLOW_RUSTC_STABLE_VERSION_RE, workflow_text)
        if result is None:
            raise ValueError("Failed to find the current version of stable rust")

        new_workflow_text = workflow_text.replace(result.group(1), version)
        with open(CHECK_WORKFLOW_PATH, "w") as f:
            f.write(new_workflow_text)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True, help="github token")
    args = parser.parse_args()

    repo = env("GITHUB_REPOSITORY")

    updater = StableUpdater(repo, args.token, branch_name="stable-neonaot", message=":arrow_up: stable", assignee="neonaot")
    updater.update()


if __name__ == '__main__':
    main()
