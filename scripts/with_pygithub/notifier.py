import argparse
from github import Github

DOC_LABEL = "To be documented"
DOC_MSG = "This message was sent automatically." # TODO change it


if __name__ == '__main__':
    print("logs: start")
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True)
    parser.add_argument("--pr_id", type=int, required=True)
    parser.add_argument("--event", type=str, required=True)
    parser.add_argument("--label", type=str)
    parser.add_argument("--repo", type=str)
    args = parser.parse_args()

    print("label = ", args.label)

    g = Github(args.token)
    repo = g.get_repo(args.repo)

    if args.event == "closed" and DOC_LABEL in [i.name for i in repo.get_pull(args.pr_id).labels] and repo.get_pull(args.pr_id).is_merged():
            print("logs: pr is closed and has needed label")
            repo.get_issue(args.pr_id).create_comment(DOC_MSG)
    elif args.event == "labeled" and args.label == DOC_LABEL  and repo.get_pull(args.pr_id).is_merged():
            print("logs: new label in merged pr ")
            repo.get_issue(args.pr_id).create_comment(DOC_MSG)
    else:
        print("logs: no need to do anything")

