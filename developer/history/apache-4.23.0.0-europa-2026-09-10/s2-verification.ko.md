<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# S2 검증 기반 및 변경 검토

부모 Epic #987 / 작업 #990 / 구현 PR #1002.

## 상태와 경계

S2의 `baseline_ready`와 전체 DONE은 별도 게이트다. 이 문서는 독립 빌드/도구/라이선스 작업의 증거를 기록한다. 원본 77개를 모두 검토했으며, 19개 merge와 기능/보안/DB/최종 릴리즈에 종속된 17개 변경은 여전히 미완료다. 이들 36개를 Applied 또는 Excluded로 대체하지 않는다. 담당 연결은 [s2-review.tsv](s2-review.tsv)를 참조한다.

- 업그레이드 출발점: 사용자 확정 `014895d8f3dc2f062f379b51ee62d36a0adae88a`.
- 구현 시작점: `bcb52804f3847c3dddebb2dc54cc93a627c52cd6`, #995 백업 후속과 #1000 추적표를 포함한다.
- 첫 라이선스 수정: `5f4c96fdb7106de051caacf7168ee344d671b182`.
- 의존성/도구 기본 반영: `8aa74e786d1416f20377579ac32182e0acd915aa`.
- Europa 전용 BC 소비자 보완: `6611e09a99`.
- 연결 누수 회귀 테스트: `d415cef44e`.
- 실제 제품 코드의 빌드/테스트와 문서 추적표 검증은 다른 증거다. DB/실물 인프라/최종 RC 통합 검증은 S4~S8에서 계속한다.

## 도구와 Rocky 9.8 경로

| 항목 | 계약 및 검증 |
| --- | --- |
| OS/CPU | Rocky Linux 9.8, linux/amd64; 다른 OS/CPU는 준비 스크립트가 거부 |
| Java | JDK17; RPM의 기존 BuildRequires를 위해 JDK11도 설치하되 JAVA_HOME은 17 |
| Maven | 3.9.10; 공식 아카이브 SHA512 고정 |
| Node/npm | 14.21.3 / 6.14.18; 공식 Node 아카이브 SHA256 고정 |
| Python | 제품 3.10, 공식 빌드 3.10.21 소스 SHA256 고정; 시스템 DNF용 Python은 교체하지 않음 |
| MySQL | 서버 8.0.46, Connector/J8.4.0 읽기 전용 SELECT 통과 |
| lint | 제품과 분리된 Actions Python3.11; Node 제품 버전 변경 없음 |

`tools/build/rocky98-prepare.sh --check`는 설치/저장소 변경 없이 도구 계약을 확인한다. 설치 모드는 일회용 Rocky9.8 컨테이너의 root에서만 사용한다. 기존 개발 컨테이너에서는 `--check`만 실행했다.

신규 `rocky98-rpm-build.sh`와 `.github/workflows/rocky98-rpm.yml`은 기존 패키징, schema resource 비교, UI buildVersion 확인 및 Storage Service 공개키 주입 계약을 유지한다. 9.7 helper는 9.8에서 DNF 변경 전에 실패하며, 실행 전후 Rocky 저장소 파일의 SHA256이 동일함을 확인했다.

Python3.10.21에 포함된 setuptools79.0.1은 sdist 이름을 `marvin-*.tar.gz`로 정규화한다. 첫 공식9.8 RPM 실행에서 기존 대문자 전용 glob 때문에 `%install`이 실패한 것을 확인했다. 공통 RPM spec의 복사/설치3곳을 `[Mm]arvin-*.tar.gz`로 바꾸어 기존53.0.0의 대문자 아카이브와 새 소문자 아카이브를 모두 지원한다. Python 버전이나 RPM 설치 경로는 변경하지 않는다.

공식 이미지: [Rocky9.8 20260525.0 x86_64 OCI](https://download.rockylinux.org/pub/rocky/9.8/images/x86_64/Rocky-9-Container-Base-9.8-20260525.0.x86_64.oci.tar.xz), SHA256 `1210df99dcf0ef4d73940244e6d703935175629598b09c0e430da20e76768402`.

산출물에는 실제 checkout SHA, OS/아키텍처, 도구 버전, 패키징 입력, 고정 nonoss SHA, RPM 의존성 목록, POM/package-lock 해시와 RPM SHA256SUMS를 남긴다. Actions run 링크에서 artifact를 다운로드하며, 실패 로그도 업로드한다. 기존 9.7 release workflow 호출자는 이번에 자동 전환하지 않았으므로, S8 공식 Europa RC는 명시적으로 **Rocky9.8 workflow**를 선택하고 동일 SHA의 설치/업그레이드를 검증해야 한다.

## 업스트림 적응

- MySQL Java 좌표는 Europa에 이미 `com.mysql:mysql-connector-j`로 정리되어 있었다. 버전8.4.0, classpath, caching_sha2 인증과 Marvin 후속을 적용했다. 업스트림의 `MYSQL_CONNECTOR_VERSION = '8.4.0'`는 셸에서 명령으로 해석되므로 공백 없는 변수 대입으로 수정했다.
- Bouncy Castle1.83/jdk18on과 MinIO8.6.0/okhttp5.1.0을 함께 적용했다. MinIO 모듈의 선언만 바꾸면 최종 client에서 공통 의존성 관리가 OkHttp4.9.3과 interceptor4.12.0을 선택하는 것을 추가로 발견했다. 루트 OkHttp/interceptor 관리를5.1.0으로 통일하여 실제 실행 파일의 버전도 맞췄다. Apache에 없는 Europa automation/rack-management POM, utils artifact copy, securitycheck JAR 참조도 수정하여 빌드를 복구했다. LDAP의 구형 BC 전이 의존성 제외를 유지한다.
- scoped config 조회의 Transaction 수명을 복구하고, 성공/예외 각각에서 연결을 닫는 테스트를 추가했다. 동일 수정의 두 upstream SHA를 중복 적용하지 않는다.
- StatsCollector 정리 작업의 RuntimeException을 기록하여 주기 작업이 영구 중단되지 않도록 하는 upstream 수정과 테스트를 적용했다.
- QemuImgTest의 네이티브 libvirt 로딩 실패는 명시적 skip으로 처리한다. 해당 컨테이너에서는 1개 skip이며 실제 KVM 기능 PASS를 의미하지 않는다.
- CI의 기존 Europa workflow_call 인터페이스는 유지하면서 공통 setup-env/install-nonoss action, JDK/Python/Maven, npm ci, SHA 고정 action 및 Europa push branch 필터를 반영한다. Apache 전용 Sonar/CodeQL/gh-aw/협업자 자동화는 그대로 이식하지 않는다.
- pre-commit 업데이트 중 기존 이미지 압축/Markdown 표 스타일을 대규모로 바꾸는 oxipng10.1.0과 markdownlint0.48.0은 현행9.1.5/0.45.0을 유지한다. 나머지 다섯 hook 버전은 반영하며 검사 자체를 제거하지 않는다. 이 결정은 새 검사 실패를 기존 실패로 오인하지 않기 위한 명시적 적응이다.

## 로컬 검증

모든 실행은 `./dev exec`를 통한 Docker 내부에서 수행했다. 원본 및 후보 UI lint는 `--no-fix`, 단위 테스트는 `--runInBand --coverage=false`다.

| 검사 | 결과 |
| --- | --- |
| 기준 backend 014895d8f3 | 161개 reactor 전체 빌드 통과; 분리된 Maven 저장소 및 native Git 옵션 사용 |
| 후보 backend | 161개 reactor 전체 빌드 통과; 테스트는 별도 실행 |
| 기준 전체 backend unit | 938 suite reports / 11,478 tests, failures0/errors0/skipped14; 161 reactor PASS |
| 후보 전체 backend unit (b7dd4539e3, HTTP 통일 전) | 같은938 suite reports / 11,484 tests, failures0/errors0/skipped14; 161 reactor PASS |
| 기준 UI lint/unit | lint 통과; 27 suites / 341 tests 통과 |
| 후보 UI lint/unit | lint 통과; 27 suites / 341 tests 통과 |
| HTTP 의존성 통일 후 | 161 reactor 재빌드 PASS; 영향 89 tests/failures0/errors0/skipped0 및 최종 JAR BC/MinIO/InfluxDB smoke PASS |
| 영향 backend 테스트 | 210 tests, failures0, errors0, skipped1; FTCTL70 및 추가 BC/TLS36 포함 |
| MySQL connector | 기존 DB 읽기 전용 SELECT 및 격리 MySQL8.0.46 caching_sha2_password 인증, Connector8.4.0 통과 |
| Marvin 변경 의존성 | Python3.10 venv에서 mysql-connector-python8.4.0/pycryptodome3.23.0 설치, import/AES roundtrip 통과 |
| 기준 RAT | 269 unknown 재현 |
| 후보 RAT | 같은0.12 검사, unknown0/unapproved0 통과 |
| Rocky platform guard | 기존9.7 helper가 9.8에서 종료1; repo 파일 해시 불변 |
| 구문/추적 | actionlint, bash -n, git diff --check, 299 SHA ledger 검사 통과 |
| 로컬 UI 배포 빌드 | 기준/후보 모두 통과; 로컬 검증용으로 productionSourceMap=false |
| Actions UI Build / License Check | b7dd4539e3 후보에서 둘 다 통과; UI는 원래 소스맵 포함 빌드 |
| Actions full backend | [Build 34449558672](https://github.com/ablecloud-team/ablestack-cloud/actions/runs/34449558672): 172 reactor PASS, 12,265 tests/failures0/errors0/skipped17 |
| Actions Rocky9.7 RPM | [기존 경로 34449558720](https://github.com/ablecloud-team/ablestack-cloud/actions/runs/34449558720) PASS; 9.8 증거와 구분 |
| Actions Rocky9.8 RPM | 실행 중; 최종 결과는 후속 기록 |

테스트 상세: [영향 테스트](s2-test-results.tsv), [모듈별 기준/후보 전체 테스트](s2-backend-suite-results.tsv). 전체 테스트는 Maven `-Pdeveloper -Dsimulator -T2 test`로 별도 실행했고, 기준은 위 빌드와 같은 분리 저장소/native Git 옵션을 사용했다. 실제 Surefire XML과 로그 집계를 대조했다. 전체 로컬 XML은 b7dd4539e3 시점의 스냅샷이며, 이후 HTTP 의존성 통일은 별도 영향 테스트와 최종 Actions에서 검증한다. 기준 대비 추가6개는 ConfigDepotImplTest2개와 StatsCollectorTest4개다. 영향 테스트210개는 전체 테스트와 중복되므로 합산하지 않는다. MinIO/LDAP는 단위 테스트이며 외부 서비스의 통합 인증은 S3/S5B에서 수행한다.

기준 build의 최초 실패는 StorPool의 구형 JGit plugin이 Git worktree의 commit을 찾지 못하는 문제였다. native Git과 분리된 Maven 저장소로 전체 reactor를 재실행하여 통과했으며, 이 실패를 제품 회귀로 기록하지 않는다. 재현 명령:

```bash
mvn -B -ntp -Dmaven.repo.local=/tmp/epic990/baseline-m2 \
  -Dmaven.gitcommitid.nativegit=true -Pdeveloper -Dsimulator -DskipTests -T2 install
```

client를 clean install하여 증분 빌드 디렉터리의 구형 BC1.70/MySQL8.0.33 JAR 잔존을 제거했다. 새 RPM 검사도 필요한 BC1.83/Connector8.4.0 JAR 존재와 구형 JAR 부재를 검사한다. 이 검사는 추가로 필요했던 실제 패키징 회귀 방지 항목이다. `ManagementRuntimeSmoke.java`도 추출된 RPM의 실제 classpath에서 실행한다. reflection으로 실제 로드된 OkHttp5.1.0을 확인하고 BC RSA/X.509 서명·검증 및 loopback HTTP 서버에 대한 MinIO bucket list/object upload, InfluxDB ping/write를 검증한다. 기존 OkHttp4.9.3을 classpath 앞에 두는 음성 대조에서는 검사 실패를 확인했다. 외부 MinIO/InfluxDB 서비스 통합 검증을 대신하는 것은 아니다.

S1 merge 요약 숫자는 재검사에서 14개 nonempty/5개 empty가 맞았다. 기존13/6 요약을 정정했으며 19개 diff의 개별 SHA256은 모두 일치했다. 빈 diff를 부모 소스 반영 완료 또는 자동 제외로 처리하지 않는다.

## RAT와 기존 실패

269개는 Java115, JS12, Vue1, Python3, shell policy2, conf1, Markdown135 파일의 누락/축약 헤더였다. 전체 코드 변경에서 주석/공백 이외 내용이 동일한지 비교했고, 재검사에 새 exclude나 skip을 추가하지 않았다. [파일별 변경 해시](s2-license-headers.tsv)를 보관한다.

기존 DB SQL99건 및 simulator FK 실패를 별도 MySQL8.0.46 컨테이너에서 **014895d8f3와 후보 b7dd4539e3 양쪽 모두 실제 재현**했다. 기존 개발 DB/볼륨은 사용하지 않았다. 기준은 분리된 Maven 저장소의 Connector8.0.33, 후보는 Connector8.4.0을 사용했다. DB용 scratch worktree에는 각 빌드가 생성한 SystemVM metadata도 준비하여 동일한 실행 조건을 맞췄다.

`mvn -Pdeveloper -pl developer -DskipTests -Ddeploydb`는 두 경우 모두 종료0이지만 SQL99건을 실패하고 `4.23.0.0 Complete`를 기록했다. 실패 SQL을 정렬한 SHA256이 동일하며 cloud382 tables+35 views, usage31 tables+1 view도 같다. 이어 `-Ddeploydb-simulator`는 양쪽 모두 종료1, template ID111의 `fk_template_store_ref__template_id` 오류와 simulator8 tables를 남겼다. [DB 비교표](s2-db-results.tsv)에 결과와 오류 SQL 해시를 기록했다. 시험용 tmpfs 컨테이너는 증거 저장 후 제거했다.

이는 기존 실패를 분리한 기준 증거이며 정상 설치/업그레이드 PASS가 아니다. S4는 저장 프로시저 분리, 복수 SQL 실행, storage_service_instance 누락, backups.extenal_id 오타와 실제 기존 데이터 업그레이드·동일 버전 재실행을 수정/검증해야 한다.

전체 저장소 lint는 S1 기준에서도 헤더/권한/깨진 링크/EOF/개행/공백/철자/Markdown 실패가 있었다. S2에서 라이선스 검사269건을 해결한 것이 전체 pre-commit 통과를 뜻하지 않는다. 변경 hook의 추가 실패와 원래 실패를 분리하여 기록하며, 전체 검사 비활성화로 통과시키지 않는다.

## 남은 S2 마감

19개 merge의 독립 remerge hunk와 17개 기능/DB/보안/릴리즈 종속 변경은 [77개 검토표](s2-review.tsv)에 담당 #991~#999와 이유를 기록했다. S2 전체 완료를 위해 이들 코드의 최종 판정/병합/검증이 필요하다. 특히 API ACL 서명, historical upgrade chain, fail-fast module startup, realhostip/SystemVM 및 최종 version stamping은 소유 단계의 구현과 함께 처리한다. `baseline_ready`가 확보되면 S3 착수는 가능하지만 #990을 자동 종료하지 않는다.
