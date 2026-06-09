### 👍 [Git깔나는 조](https://app.notion.com/p/ohgiraffers/1-Git-333649136c1180428238fe021b5b109e) [![codecov](https://codecov.io/gh/sb08-WardrobeAssistant-teamGitGgal/sb08-WardrobeAssistant-teamGitGgal/graph/badge.svg?token=VLATROMN0N)](https://codecov.io/gh/sb08-WardrobeAssistant-teamGitGgal/sb08-WardrobeAssistant-teamGitGgal)

***

### 👥 팀원 구성

| <img width="72" height="102" alt="이예은" src="https://github.com/user-attachments/assets/a82f4598-18c3-4156-8361-fa160089c835" /> | <img width="72" height="102" alt="현승원" src="https://github.com/user-attachments/assets/c1d5aa02-1763-4ddb-a636-350b6cfa5b08" /> | <img width="72" height="102" alt="박성조" src="https://github.com/user-attachments/assets/903f59b8-4d7a-4d4c-9227-953031d958d3" /> | <img width="72" height="102" alt="박하민" src="https://github.com/user-attachments/assets/fc03733c-dacd-47a9-a323-ef7879ea470d" /> | <img width="72" height="102" alt="최준영" src="https://github.com/user-attachments/assets/8ec06e64-749f-491f-9148-894f79d9032d" /> |
| --- | --- | --- | --- | --- |
| [**이예은**](https://github.com/yeeun000) | [**현승원**](https://github.com/seungwon00) | [**박성조**](https://github.com/castle-bird) | [**박하민**](https://github.com/parkhamin) | [**최준영**](https://github.com/Junkov0) |

---

### 👕 프로젝트 소개
<img width="900" height="450" alt="image" src="https://github.com/user-attachments/assets/d385a4e6-5887-4cd3-b944-c820f0686eea" />

- 제목 : 옷장을 부탁해
- 부제 : 개인화 의상 및 아이템 추천 SaaS
- 소개 : 날씨, 취향을 고려해 사용자가 보유한 의상 조합을 추천해주고, OOTD 피드, 팔로우 등의 소셜 기능을 갖춘 서비스
- 기간 : 2026.04.29(수) - 2026.06.12(월)
---

### 🔨 기술 스택

| 카테고리 | 기술 |
|---|---|
| Language | ![Java](https://img.shields.io/badge/Java_17-007396?style=flat&logo=openjdk&logoColor=white) |
| Framework | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white) ![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat&logo=spring&logoColor=white) ![Spring WebFlux](https://img.shields.io/badge/Spring_WebFlux-6DB33F?style=flat&logo=spring&logoColor=white) |
| ORM / Query | ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat&logo=spring&logoColor=white) ![QueryDSL](https://img.shields.io/badge/QueryDSL-0089CF?style=flat) |
| DB | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white) ![H2](https://img.shields.io/badge/H2-0000BB?style=flat) |
| Migration | ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat&logo=flyway&logoColor=white) |
| Search | ![OpenSearch](https://img.shields.io/badge/OpenSearch-005EB8?style=flat&logo=opensearch&logoColor=white) |
| Cache | ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white) |
| Messaging | ![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat&logo=apachekafka&logoColor=white) |
| Security | ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white) ![OAuth2](https://img.shields.io/badge/OAuth2-EB5424?style=flat&logo=auth0&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white) |
| Storage | ![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white) |
| WebSocket | ![WebSocket](https://img.shields.io/badge/WebSocket_STOMP-010101?style=flat&logo=socket.io&logoColor=white) |
| AI | ![OpenAI](https://img.shields.io/badge/Spring_AI_(OpenAI)-412991?style=flat&logo=openai&logoColor=white) |
| API Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black) |
| Build | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white) |
| Test | ![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=flat&logo=junit5&logoColor=white) ![JaCoCo](https://img.shields.io/badge/JaCoCo-E25C00?style=flat) |


---

### 🔳 ERD 설계

<img width="900" height="450" alt="image" src="https://github.com/user-attachments/assets/40747659-826e-45a3-ab9a-13360705b4cc" />

---

### 🌴 AWS 아키텍쳐

<img width="900" height="500" alt="image" src="https://github.com/user-attachments/assets/77736a50-3fcc-451e-9f1b-2aef55e5871e" />

---

### 👀 구현 기능 상세

#### 이예은(사용자)
<img width="900" height="450" alt="사용자 (1)" src="https://github.com/user-attachments/assets/cd5dc03d-a6b8-4e12-8e4e-2ab5ce2d9287" />

<details>
<summary>어드민 기능</summary>
  
- 초기화 : 서버 시작 시 어드민 계정을 자동으로 초기화
- 권한 관리 : 사용자의 권한을 ADMIN 또는 USER로 변경, 권한 변경 시 해당 사용자는 자동으로 로그아웃
- 계정 잠금 : 사용자 계정을 잠글 수 있으며, 잠긴 계정은 로그인할 수 없음, 잠긴 계정은 자동으로 로그아웃
</details>
<details>
<summary>회원가입</summary>
  
- 이름, 이메일, 비밀번호를 입력하여 회원 가입
</details>
<details>
<summary>로그인</summary>
  
- 이메일과 비밀번호를 입력하여 로그인
- JWT 기반 인증/인가를 사용
- 기존에 로그인된 계정이 있을 경우, 강제로 로그아웃 처리
</details>
<details>
<summary>비밀번호 초기화</summary>
  
- 이메일 주소로 임시 비밀번호를 발급
- 임시 비밀번호는 만료시간(3분)을 가지며, 만료되기 전까지 임시 비밀번호로 로그인 가능
- 사용자가 비밀번호를 재설정하면, 임시 비밀번호는 파기
</details>
<details>
<summary>소셜 계정 연동</summary>
  
- Google, Kakao 계정을 연동
- 계정 연동 후 해당 소셜 계정으로 로그인
</details>

### 현승원(날씨)
<img width="900" height="506" alt="날씨" src="https://github.com/user-attachments/assets/d7a76f09-83ee-4967-94f9-4e8b1ab00a3f" />

<details>
<summary>날씨 데이터 관리</summary>
  
- [기상청 단기 예보](https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15084084) Open API를 활용해 날씨 데이터를 수집합니다.
- [**카카오 API**](https://developers.kakao.com/docs/ko/local/dev-guide#coord-to-district)를 활용하여 행정 구역 이름을 나타냅니다.
</details>
<details>
<summary>특별한 날씨 알림</summary>
  
- 비가 오거나, 갑자기 온도가 상승하는 등 **특별한 날씨 변화 발생 시 사용자에게 알림**을 보냅니다.
</details>

### 박성조(프로필, 의상)
<img width="900" height="450" alt="박성조" src="https://github.com/user-attachments/assets/ae6ee564-3e0f-4503-b119-94589a3503c0" />

<details>
<summary>프로필 관리</summary>
  
- 사용자의 기본정보(이름, 성별, 위치정보, 날씨 민감도)를 수정/삭제 가능할 수 있습니다.
- 프로필 이미지을 수정할 수 있습니다.
</details>
<details>
<summary>의상 관리</summary>
  
- 사용자가 소지한 의상들을 추가/수정/삭제할 수 있습니다.
- 의상 카테고리에 따라 의상 목록을 조회할 수 있습니다.
</details>
<details>
<summary>AI를 이용한 의상 추천/추출</summary>
  
- **OpenAI API +** **현재 날씨 + 사용자가 소유한 의상 + 사용자 날씨 민감도**를 이용해 의상 추천을 받을 수 있습니다.
- 무신사, 지그재그 같은 **쇼핑몰의 URL을 통해서 의상 정보를 추출**할 수 있습니다.
- 위 기능들로 인해서 의상을 편하게 등록하고, 적절하게 추천받을 수 있습니다.
</details>

### 박하민(피드)
<img width="900" height="450" alt="피드피드" src="https://github.com/user-attachments/assets/6c983619-fc3c-452b-b8dc-583e6e76303c" />

<details>
<summary>피드 CRUD</summary>
  
- 피드 생성/수정/삭제
- 피드 데이터가 변경될 때마다 ES 인덱싱 작업 수행
</details>
<details>
<summary>피드 목록 조회</summary>
  
- 전체 피드 및 팔로잉 사용자 피드 조회
- 특정 사용자가 작성한 피드 조회
- 생성시간·좋아요 순 정렬
- 커서 기반 페이지네이션
- Elasticsearch 기반 키워드 검색 연동(형태 변형/부분 일치 지원)
- 강수, 날씨 등 날씨 조건 필터링
</details>
<details>
<summary>피드 좋아요/댓글</summary>
  
- 피드 좋아요/좋아요 취소
- 댓글 등록 및 목록 조회(최신순 정렬 고정)
</details>

### 최준영(알림/팔로우/DM)
<img width="900" height="450" alt="DM팔로우알림 (1) (1)" src="https://github.com/user-attachments/assets/b05773e0-4160-4ca1-af29-89b62c2ad1ff" />

<details>
<summary>팔로우</summary>
  
- 다른 사용자를 팔로우/언팔로우 가능
- 팔로잉 및 팔로워 목록 조회
- 이름 검색 및 커서 기반 페이지네이션 지원
- 특정 사용자의 팔로워 수, 팔로잉 수, 현재 로그인 사용자의 팔로우 여부 확인 (팔로우 요약 조회)
</details>
<details>
<summary>DM(다이렉트 메시지)</summary>
  
- WebSocket(STOMP) 기반 실시간 메시지 송수신
- JWT 기반 WebSocket 인증 처리
- 대화 내역 커서 기반 페이지네이션 조회
</details>
<details>
<summary>실시간 알림</summary>

- SSE(Server-Sent Events) 기반 실시간 알림 수신
- 팔로우, DM, 피드(좋아요·댓글·생성), 의상 변경, 권한 변경 등 다양한 이벤트 발생 시 알림 전송
- Redis Pub/Sub을 통해 어느 서버 인스턴스에 연결된 사용자에게도 알림 (다중 인스턴스 환경)
</details>

---

### 🗂️ 프로젝트 구조
```
.
  ├── .github/
  │   ├── workflows/
  │   │   ├── cd.yaml
  │   │   └── ci.yaml
  │   └── PULL_REQUEST_TEMPLATE.md
  ├── src/
  │   ├── main/
  │   │   ├── java/com/gitggal/clothesplz/
  │   │   │   ├── component/
  │   │   │   │   ├── batch/
  │   │   │   │   ├── publisher/
  │   │   │   │   └── subscriber/
  │   │   │   ├── config/
  │   │   │   ├── controller/
  │   │   │   │   ├── auth/
  │   │   │   │   ├── clothes/
  │   │   │   │   ├── feed/
  │   │   │   │   ├── follow/
  │   │   │   │   ├── message/
  │   │   │   │   ├── notification/
  │   │   │   │   ├── profile/
  │   │   │   │   ├── user/
  │   │   │   │   └── weather/
  │   │   │   ├── dto/
  │   │   │   ├── entity/
  │   │   │   ├── event/
  │   │   │   ├── exception/
  │   │   │   ├── mapper/
  │   │   │   ├── repository/
  │   │   │   ├── security/
  │   │   │   │   ├── jwt/
  │   │   │   │   ├── oauth/
  │   │   │   │   └── websocket/
  │   │   │   ├── service/
  │   │   │   └── util/
  │   │   └── resources/
  │   └── test/
  │       ├── java/com/gitggal/clothesplz/
  │       │   ├── component/
  │       │   ├── controller/
  │       │   ├── mapper/
  │       │   ├── repository/
  │       │   ├── security/
  │       │   ├── service/
  │       │   └── support/
  │       └── resources/
  ├── .gitattributes
  ├── .gitignore
  ├── .dockerignore
  ├── build.gradle
  ├── docker-compose.yaml
  ├── Dockerfile
  ├── gradlew
  └── settings.gradle
```
---
### ⛵ [구현 홈페이지](https://clothesplz.kro.kr/)

---

### 📜 [SWAGGER](https://clothesplz.kro.kr//swagger-ui/index.html)

---

### 📺 [프로젝트 회고록]()
- 발표 자료 완성 후 pdf 첨부 예정
