import argparse
from github import Github

DOC_LABEL = "To be documented"
DOC_MSG = "this message was sent automatically."


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True)
    parser.add_argument("--pr_id", type=int, required=True)
    parser.add_argument("--event", type=str, required=True)
    parser.add_argument("--label", type=str)
    args = parser.parse_args()

    # TODO remove it
    print(args.label)

    g = Github(args.token)
    repo = g.get_repo("neonaot/intellij-rust")

    if args.event == "merging":
        if DOC_LABEL in [i.name for i in repo.get_pull(args.pr_id).labels]:
            repo.get_issue(args.pr_id).create_comment(DOC_MSG)
    elif args.event == "labeling": # and args.label == DOC_LABEL: #TODO
#         if repo.get_pull(args.pr_id).is_merged():
          repo.get_issue(args.pr_id).create_comment(DOC_MSG)
    else:
        print("wrong event type")





