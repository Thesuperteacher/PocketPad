"""Check the files and history intended for publication. Requires Python 3.9+."""
import argparse
import io
import re
import subprocess
import sys
import zipfile
from pathlib import Path

PATTERNS = {
    "private key": rb"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----",
    "GitHub token": rb"\bgh[pousr]_[A-Za-z0-9]{30,}\b|\bgithub_pat_[A-Za-z0-9_]{40,}\b",
    "AWS access key": rb"\bAKIA[0-9A-Z]{16}\b",
    "Slack token": rb"\bxox[baprs]-[A-Za-z0-9-]{20,}\b",
    "Google API key": rb"\bAIza[A-Za-z0-9_-]{35}\b",
    "pairing credential": rb"pocketpad://[^\s\"<>]*[?&](?:token|key)=[a-fA-F0-9]{48}",
    "personal Windows path": rb"[A-Za-z]:[\\/]+Users[\\/]+(?!Public\b|Default\b)[A-Za-z0-9_.-]+[\\/]",
}


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args])


def scan_bytes(name, data, depth=0):
    issues = []
    representations = [data]
    if b"\0" in data:
        representations.append(data.decode("utf-16le", errors="ignore").encode("utf-8"))
    for label, pattern in PATTERNS.items():
        if any(re.search(pattern, value) for value in representations):
            issues.append(f"{name}: {label}")  # Never echo the matching secret.
    if data.startswith(b"PK\x03\x04"):
        if depth >= 4:
            return issues + [f"{name}: archive nesting exceeds review limit"]
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                members = archive.infolist()
                if len(members) > 20000 or sum(m.file_size for m in members) > 500_000_000:
                    return issues + [f"{name}: archive exceeds review limit"]
                for member in members:
                    if not member.is_dir():
                        issues.extend(scan_bytes(f"{name}!{member.filename}", archive.read(member), depth + 1))
        except (zipfile.BadZipFile, RuntimeError):
            issues.append(f"{name}: unreadable archive")
    return issues


def verify(root, staged=False):
    allowed = set((root / "publication-files.txt").read_text(encoding="utf-8").splitlines())
    issues, checked = [], set()
    refs = [None] if staged else git(root, "rev-list", "--all").decode().splitlines()
    if not refs:
        return ["No commits to check; use --staged before the first commit."]
    for ref in refs:
        if ref is None:
            paths = git(root, "ls-files", "-z").decode().split("\0")
        else:
            paths = git(root, "ls-tree", "-r", "--name-only", "-z", ref).decode().split("\0")
        for path in filter(None, paths):
            if path not in allowed:
                issues.append(f"Unexpected publication file: {path}")
                continue
            spec = f":{path}" if ref is None else f"{ref}:{path}"
            blob = git(root, "rev-parse", spec).strip()
            if blob not in checked:
                checked.add(blob)
                issues.extend(scan_bytes(path, git(root, "show", spec)))
    return sorted(set(issues))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staged", action="store_true", help="Check the index before committing")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    issues = verify(root, args.staged)
    if issues:
        print("Publication check failed:\n" + "\n".join(issues), file=sys.stderr)
        return 1
    print("Publication check passed: allowlisted files and credential patterns checked.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
