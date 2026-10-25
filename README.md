# codedlines-team11
hello world
from types import SimpleNamespace
from unittest.mock import Mock

from services.github_client import GitHubClient


def test_analyze_repo_reads_files_from_current_branch_head():
    current_head = ["first-head"]
    contents_by_head = {
        "first-head": b"print('old version')",
        "second-head": b"print('updated version')",
    }
    repo = Mock()
    repo.default_branch = "main"
    repo.name = "demo"
    repo.owner = SimpleNamespace(login="example")
    repo.get_branches.return_value = [SimpleNamespace(name="main")]
    repo.get_branch.side_effect = lambda _branch: SimpleNamespace(
        commit=SimpleNamespace(sha=current_head[0])
    )
    repo.get_commits.return_value = []
    repo.get_git_tree.side_effect = lambda ref, recursive: SimpleNamespace(
        tree=[
            SimpleNamespace(type="blob", path="src/app.py", sha=f"blob-{ref}")
        ]
    )
    repo.get_contents.side_effect = lambda path, ref: SimpleNamespace(
        decoded_content=contents_by_head[ref]
    )

    client = GitHubClient()
    first_analysis = client.analyze_repo(repo, branch="main")
    current_head[0] = "second-head"
    second_analysis = client.analyze_repo(repo, branch="main")

    assert first_analysis.files[0].content == "print('old version')"
    assert second_analysis.files[0].content == "print('updated version')"
    assert [call.args[0] for call in repo.get_git_tree.call_args_list] == [
        "first-head",
        "second-head",
    ]
    assert [call.kwargs["ref"] for call in repo.get_contents.call_args_list] == [
        "first-head",
        "second-head",
    ]
