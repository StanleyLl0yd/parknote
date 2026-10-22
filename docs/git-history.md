# Original local Git history

The source project originally contained three commits, with HEAD `76a536c0d7718f40dc2c46bdbbf2da856a2adbcf`. Their exact commit SHA identities were not preserved when source snapshots were replayed through the connected GitHub API to initialize this repository. The original objects and relationships are preserved in [ParkNote-history-2026-10-22.bundle](../archive/ParkNote-history-2026-10-22.bundle).

- Initial commit: `655217c402d9eccf3d8031f730eac43e0cf68491`.
- Second commit: `c5e15d0703e1e3afbd3ae4f4c130a7ba49002db1`.
- Final original commit: `76a536c0d7718f40dc2c46bdbbf2da856a2adbcf`.
- Bundle SHA-256: `749f20814c9b7ef760c9134df27a40420aead6745f5071ef3e893adde072292d`.

To inspect the exact original commits in a separate local branch from a clone of this repo:

```bash
git fetch ./archive/ParkNote-history-2026-10-22.bundle refs/heads/main:refs/heads/original-local
git log --oneline original-local
```

Do not force-push this branch to replace the protected current `main`: GitHub now has a separate, linear development history containing the same original source revisions followed by CI fixes.
