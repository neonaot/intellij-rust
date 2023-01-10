import argparse
import re

from common import execute_command, env
from updater import UpdaterBase

WORKFLOW_PATH = ".github/workflows/get-rust-versions.yml"
RUSTC_VERSION_RE = re.compile(r"[0-9][.][0-9]{2}[.][0-9]")
WORKFLOW_RUSTC_VERSION_RE = re.compile(r'(?<=STABLE: ")([0-9][.][0-9]{2}[.][0-9])')


class StableUpdater(UpdaterBase):

    def _update_locally(self):
        execute_command("rustup", "default", "stable")

        output = execute_command("rustc", "-V")
        match_result = RUSTC_VERSION_RE.search(output)

        version = match_result.group(0)
        with open(WORKFLOW_PATH) as f:
            workflow_text = f.read()

        result = re.search(WORKFLOW_RUSTC_VERSION_RE, workflow_text)
        if result is None:
            raise ValueError("Failed to find the current version of stable rust")

        new_workflow_text = re.sub(WORKFLOW_RUSTC_VERSION_RE, version, workflow_text)
        if new_workflow_text == workflow_text:
            print("The latest stable rustc version is already used")
            return

        with open(WORKFLOW_PATH, "w") as f:
            f.write(new_workflow_text)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True, help="github token")
    args = parser.parse_args()

    repo = env("GITHUB_REPOSITORY")

    updater = StableUpdater(repo, args.token, branch_name="stable-alina", message=":arrow_up: stable", assignee="neonaot")
    updater.update()


if __name__ == '__main__':
    main()
