import argparse

from common import execute_command, env
from updater import UpdaterBase


class testcommitUpdater(UpdaterBase):
    def _update_locally(self) -> None:
        file = open('testfile.txt', 'w')
        file.write("hello")
        file.close()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True, help="github token")
    args = parser.parse_args()

    repo = env("GITHUB_REPOSITORY")

    updater = testcommitUpdater(repo, args.token, branch_name="alina-test-branch",
                                message="test files", assignee="neonaot")
    updater.update()


if __name__ == '__main__':
    main()
