# PoC GIF 캡처 가이드

레드팀 보고서에 넣을 "PoC 실행 GIF"를 일관된 포맷으로 만들기 위한 절차.
목표: **검증자가 GIF만 봐도 "공격이 실제로 LLM/도구를 탈취한다"를 이해**하게.

## 권장 도구

| OS | 도구 | 비고 |
|---|---|---|
| Windows | [ScreenToGif](https://www.screentogif.com/) | 영역 지정·프레임 편집. 추천. |
| macOS | [Kap](https://getkap.co/) | GIF/MP4. |
| 크로스(터미널) | [asciinema](https://asciinema.org/) + [agg](https://github.com/asciinema/agg) | 터미널 데모는 이게 최고. 작고 선명. |

터미널 데모(검증 스크립트)는 asciinema+agg, 웹 데모(:8090) 같은 GUI는 ScreenToGif/Kap.

## 저장 위치 / 네이밍

```
docs/assets/rt-002.gif
docs/assets/rt-003.gif
docs/assets/rt-004.gif
docs/assets/rt-006.gif
docs/assets/verify-all.gif    # 마스터 검증 1컷 (대표)
docs/assets/web-demo.gif      # 웹 UI (선택)
```

readme 참조: `![RT-004 PoC](docs/assets/rt-004.gif)`

## 캡처 시나리오 (RT별 1개, 15~30초, "정상→공격→누설")

### 공통 준비
```bash
bash start-all.sh
clear
```

### RT-002 — description poisoning × IDOR
```bash
bash scripts/rt-002-stage1.sh
```
> Case 3에서 `VIP 의전용` 누설 + `RT-002 Stage 1 PASS`.

### RT-003 — cross-server desc injection × /etc/passwd
```bash
bash scripts/rt-003-stage1.sh
```
> Case 3에서 `root:x:0` 노출.

### RT-004 — tool-output injection × PII sink (tool-레벨)
```bash
bash scripts/rt-004-stage1.sh
```
> tools/list에 `getExternalInstitutionRecord`/`poisonedTool` → 호출 응답에 `HACKED!!`.

### RT-006 — tool-output prompt injection (host e2e)
```bash
bash scripts/rt-006-stage1.sh
```
> Case 3에서 host가 output 따라 read_file 후속 호출 → `root:x:0`.

### 마스터 검증 1컷 (대표 GIF)
```bash
bash scripts/verify-all.sh
```
> 마지막 `VERIFY-ALL SUMMARY` 표 + `ALL STAGES PASS`.

## 팁
- 터미널 16~18pt, 폭 100컬럼 — 썸네일에서 글자 읽혀야 함.
- PASS(초록)/FAIL(빨강) 대비가 살면 설득력↑.
- 전부 mock/가짜 데이터지만, 화면에 실제 API 키 보이면 잘라낼 것.
- GIF 5MB 이하 권장(GitHub 로딩). 길면 asciinema 링크로 대체.

## asciinema 예시
```bash
asciinema rec rt-004.cast -c "bash scripts/rt-004-stage1.sh"
agg --cols 100 --rows 30 rt-004.cast docs/assets/rt-004.gif
```
