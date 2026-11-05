#!/usr/bin/env python3
"""Run the installed-jar AE2S checks with the official Cleanroom 0.6.8-alpha launcher."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

REPO = Path(__file__).resolve().parents[1]
WORKSPACE = REPO.parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("side", choices=["server", "client"])
    parser.add_argument("--name", required=True)
    parser.add_argument("--dependencies", type=Path, required=True,
                        help="Directory containing jars listed in dependencies.json")
    parser.add_argument("--dependency-lock", type=Path, default=REPO / "verification/dependencies.json",
                        help="Checksum manifest for this dependency combination")
    parser.add_argument("--launcher", type=Path,
                        default=REPO / "verification/launch.py")
    parser.add_argument("--port", type=int, default=25841)
    parser.add_argument("--cleanroom-version", choices=["0.6.8-alpha", "0.6.13-alpha", "0.7.14-alpha"], default="0.6.8-alpha")
    parser.add_argument("--property", action="append", default=[], help="Additional verification JVM key=value")
    args = parser.parse_args()
    if not args.name or any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
                            for c in args.name):
        parser.error("Invalid scenario name")
    mods = []
    for entry in json.loads(args.dependency_lock.read_text()):
        jar = args.dependencies.resolve() / entry["file"]
        if hashlib.sha256(jar.read_bytes()).hexdigest() != entry["sha256"]:
            parser.error("Dependency checksum mismatch: " + str(jar))
        mods.append(jar)
    mods += [REPO / "build/libs/mmce_complement-1.6.1.jar",
             REPO / "build/verification/mmce-complement-verification-srg.jar"]
    for jar in mods:
        if not jar.is_file():
            parser.error("Missing installation jar: " + str(jar))
    config = WORKSPACE / ".dev/ae2-addons-verification" / args.name / args.side / (
        "config/modularmachinery/machinery")
    config.mkdir(parents=True, exist_ok=True)
    for fixture in (REPO / "verification/machinery").glob("*.json"):
        shutil.copyfile(fixture, config / ("mmce_complement_verification_" + fixture.name))
    command = [sys.executable, str(args.launcher.resolve()), args.side,
               "--name", args.name, "--port", str(args.port), "--timeout", "300", "--cleanroom-version", args.cleanroom_version,
               "--report", "verification-report.json"]
    for jar in mods:
        command += ["--mod", str(jar)]
    for prop in args.property:
        command += ["--property", prop]
    return subprocess.call(command)


if __name__ == "__main__":
    raise SystemExit(main())
