# Ablestack Veeam + Mold 백업 (호스트 / datadisk)

## 핵심

| 항목 | 값 |
|------|-----|
| 모드 | **host** — KVM Agent + `/tmp/mold/veeam` |
| Mold 저장 | **datadisk** (`/data/backup`) — Mold Veeam/NAS repository 미사용 |
| Veeam Job | **Veeam UI**에서 생성·Pre/Post 등록 (PowerShell 자동화 없음) |
| FLR→Mold | KVM `mold-veeam-restore-agent.timer` → `restoreBackup` |

## 최초 설정 (KVM)

```bash
/etc/ablestack/veeam/veeam_config.sh \
  --job-name "Mold ablecube31-2" \
  --offering-name "VeeamBackup" \
  --mold-url "http://10.10.31.20:8080/client/api" \
  --api-key KEY --api-secret 'SECRET' \
  --zone-id ZONE_UUID \
  --vm-include "*" \
  --kvm-host "10.10.31.2" \
  --backup-mode host \
  --install

bash /etc/ablestack/veeam/setup-datadisk-veeam-backup.sh --env-file /etc/ablestack/veeam/mold-backup.env
```

## Veeam UI

1. KVM에 Linux Agent Job 생성 (SelectedFiles: `/tmp/mold/veeam`)
2. Guest Processing Pre/Post:
   - Pre: `/etc/ablestack/veeam/ablestack_veeam_pre_notify.sh`
   - Post: `/etc/ablestack/veeam/ablestack_veeam_post_notify.sh`
3. FLR 후 Mold 반영: `enable-veeam-mold-restore.sh`

## 배포

| 목적 | 스크립트 |
|------|----------|
| KVM 훅 설치 | `push-to-kvm.sh` → `install.sh` |
| 설정 | `veeam_config.sh` / `setup-datadisk-veeam-backup.sh` |
| UI FLR→Mold | `enable-veeam-mold-restore.sh` |
