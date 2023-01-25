import argparse
import requests
import json
from github import Github

from common import get_patch_version, env

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True)
    parser.add_argument("--webhook", type=str, required=True)
    parser.add_argument("--slack_ids", type=str, required=True)
    args = parser.parse_args()

    slack_ids = json.loads(args.slack_ids)

    current_release_version = str(get_patch_version() - 1)

    g = Github(args.token)
    repo = g.get_repo("intellij-rust/intellij-rust")  # TODO use env

    try:
        milestone = list(filter(lambda e: current_release_version in e.title, repo.get_milestones(state="open")))[0]
    except IndexError:
        print("Can't find current milestone")
        exit()

    release_manager = milestone.description.split("@")[1]  # TODO use re?
    link = f"https://github.com/intellij-rust/intellij-rust/milestone/{milestone.number}"
    date = milestone.due_on.date()
    amount = milestone.closed_issues
    slack_user = slack_ids.get(release_manager)
    message = {
        "text": f'Release branch for v{current_release_version} is created!\n'
                f'{link}\n'
                f'Release is planned for {date} and has {amount} merged PRs!',
        "userid": slack_user,
    }

    requests.post(args.webhook, json.dumps(message))
