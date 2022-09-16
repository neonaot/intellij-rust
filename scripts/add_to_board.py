import argparse

from github import Github

from common import env

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True)
    parser.add_argument("--pull_request", type=int, required=True)
    args = parser.parse_args()

    repo = env("GITHUB_REPOSITORY")

    g = Github(args.token)
    repo = g.get_repo(repo)
    pr = repo.get_pull(args.pull_request)

    try:
        project = repo.get_projects()[0]
    except:
        # TODO
        print("Cannot find project")
        print(pr.title)
        exit()

    column_id = None
    if pr.is_merged():
        column_id = 1
    else:
        column_id = 0

    column = project.get_columns()[column_id]
    column.create_card(content_id=pr.id, content_type="PullRequest")
