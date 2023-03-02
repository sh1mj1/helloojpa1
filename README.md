# ==== 1. JPA 소개 ====

- SQL 중심적인 개발의 문제점
- JPA 소개

## 1. **SQL 중심적인 개발의 문제점**

관계형 DB (Oracle, MySQL)에서는 SQL만 사용할 수 있으므로 SQL 의존적인 개발을 할 수 밖에 없습니다.

관계형 DB의 목적과 객체지향 프로그래밍의 목적이 일치하지 않습니다. 그러나, 객체를 저장할 수 있는 가장 현실적인 방안은 관계형 DB입니다.

### **객체와 관계형 데이터베이스의 차이**

 **1. 상속**

![https://user-images.githubusercontent.com/52024566/132992161-2db24f9c-3106-4d50-bdf1-9b83088d63a3.png](https://user-images.githubusercontent.com/52024566/132992161-2db24f9c-3106-4d50-bdf1-9b83088d63a3.png)

객체의 상속관계와 유사한 관계형 데이터베이스의 개념으로 Table 슈퍼타입, 서브타입 관계가 있습니다.

객체 상속 관계에서는 `extends` 나 `implements` 로 상속 관계를 맺고 캐스팅도 자유롭습니다.

하지만 상속받은 객체(`Album`, `Movie`, `Book`) 을 데이터베이스에 저장하려면 아래 처럼 해야 합니다.

- Album 객체와 Item 객체를 분리.
- Item Table 에 하나의 쿼리, Album Table 에 하나의 쿼리를 작성해서 저장함.

그리고 Album 객체를 데이터베이스에서 조회하려면 ITEM 과 ALUBM 을 JOIN 해서 가져온 후 조회한 필드를 각 객체에 매핑시켜서 가져와야 합니다.

즉, DB 에 저장할 객체는 상속관계를 쓰지 않습니다.

 **2. 연관관계**

![https://user-images.githubusercontent.com/52024566/132992295-e91aa5be-9080-47de-b9e5-efa4ab6a40a2.png](https://user-images.githubusercontent.com/52024566/132992295-e91aa5be-9080-47de-b9e5-efa4ab6a40a2.png)

- 객체는 참조를 사용 : `member.getTeam()`
- 테이블은 외래 키를 사용 : `JOIN ON M.TEAM_ID = T.TEAM_ID`
    - MEMBER 와 TEAM 이 서로를 참조할 FK 를 둘 다 가지고 있기 때문에 양측에서 참조가 가능함.

 **3. 객체를 테이블에 맞추어 모델링**

```java
class Member {
	String id;		// MEMBER_ID
	Long teamId;	// TEAM_ID FK
  String username;// USERNAME
}

class Team {
	Long id;		// TEAM_ID PK
	String name;	// NAME
}
```

```java
INSERT INTO MEMBER(MEMBER_ID, TEAM_ID, USERNAME) VALUES ...
INSERT INTO TEAM(TEAM_ID, NAME) VALUES...
```

그런데 위 코드는 전혀 객체지향적이지 못합니다. Member 에서 Team 을 참조하는데 참조값을 가지고 있지 않습니다.

 **4. 객체다운 모델링**

```java
class Member {
	String id;		// MEMBER_ID
	Team team;		// 참조로 연관관계를 맺는다
  String username;// USERNAME
    
  Team getTeam() {
      return team;
  }
}

class Team {
	Long id;		// TEAM_ID PK
	String name;	// NAME
}
```

**객체 모델링 조회**

```sql
SELECT M.*, T.*
  FROM MEMBER M
  JOIN TEAM T ON M.TEAM_ID = T.TEAM_ID
```

```java
public Member find(String memberId) {
    // SQL 실행

    Member member = new Member();
    // 데이터베이스에서 조회한 회원 관련 정보 입력
		// ....

    Team team = new Team();
    // 데이터베이스에서 조회한 팀 관련 정보 입력
		// ....
    
    // 회원과 팀 관계 설정
    member.setTeam(team);
    return member;
}
```

이는 굉장히 번거로운 작업입니다.

**객체 그래프 탐색**

객체는 자유롭게 객체 그래프를 탐색할 수 있어야 합니다. 

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/a3a34075-20e2-4128-bd37-3214dbac4915/Untitled.png)

위 그림에서 Member 객체에서 엔티티 그래프를 통해서 Category 까지도 접근이 가능해야 하지요.

하지만 처음 실행하는 SQL에 따라 탐색 범위가 결정되기 때문에 SQL 중심적인 개발에서는 그것이 불가능합니다. 그로 인해 엔티티 신뢰 문제가 발생합니다.

**엔티티 신뢰 문제**

```java
class MemberService {
		...
    public void process() {
        Member member = memberDAO.find(memberId);
        member.getTeam(); // 사용 가능한가?
        member.getOrder().getDelivery(); // 사용 가능한가?
    }
}
```

SQL에서 탐색된 객체 이외에는 사용할 수 없으므로 엔티티를 신뢰할 수 없습니다. 

계층 아키텍쳐에서는 이전 계층에서 넘어온 내용을 신뢰할 수 있어야 하는데 SQL 중심적인 개발에서는 그것이 불가능합니다.

그렇다고 모든 객체를 미리 로딩할 수 없으므로 상황에 따라 동일한 회원 조회 메서드를 여러 번 생성해야 합니다.

즉, 이렇게 해도 진정한 의미의 계층 분할이 어렵습니다.

 **5. 같은 식별자로 조회한 두 객체 비교**

```java
String memberId = "100";
Member member1 = memberDAO.getMember(memberId);
Member member2 = memberDAO.getMember(memberId);

member1 == member2; // 다르다.

class MemberDAO{
		public Member getMember(String memberId){
				String sql = "SELECT * FROM MEMBER WHERE MEMBER_ID = ?";
				....
				// JDBC API, SQL 실행
				return new Member(...);
		}
}
```

```java
String memberId = "100";
Member member1 = list.get(memberId);
Member member2 = list.get(memberId);

member1 == member2; // 같다.
```

SQL 중심적인 개발에서는 `getMember()`을 호출할 때 `New Member()`로 객체를 생성하기 때문에 `member1`과 `member2`가 다릅니다. 

그러나 자바 컬렉션에서 조회할 경우 `member1`과 `member2`의 참조 값이 같기 때문에 두 객체는 같습니다.

**이렇게 객체를 객체답게 모델링할수록 매핑 작업만 늘어나고 side effect 만 커집니다. 이 부분을 JPA 가 깔끔하게 해결해줍니다.**

**참고 - 동일성과 동등성(Identical & Equality)**

> 자바에서 두 개의 오브젝트 혹은 값이 ‘같다’ 라는 말은 주의해서 사용해야 합니다.
동일한(identical) 오브젝트라는 말은 같은 참조(reference) 을 바라보고 있는 객체라는 말로 실제로는 하나의 오브젝트라는 의미.
동등한(equivalent) 오브젝트라는 말은 같은 내용을 담고 있는 객체라는 의미.
> 

## 2. **JPA 소개**

### **JPA**

- Java Persistence API
- 자바 진영의 **ORM** 기술 표준

### **ORM?**

- Object-relational mapping(객체 관계 매핑)
- 객체는 객체대로 설계
- 관계형 데이터베이스는 관계형 데이터베이스대로 설계
- ORM 프레임워크가 중간에서 매핑
- 대중적인 언어에는 대부분 ORM 기술이 존재

### **JPA는 애플리케이션과 JDBC 사이에서 동작**

**저장**

![https://user-images.githubusercontent.com/52024566/132992893-ccfa7103-2a55-4f81-80c2-4e4bd269fefd.png](https://user-images.githubusercontent.com/52024566/132992893-ccfa7103-2a55-4f81-80c2-4e4bd269fefd.png)

**조회**

![https://user-images.githubusercontent.com/52024566/132992894-d55e1e4b-5833-44cc-bb30-4a1006a840ac.png](https://user-images.githubusercontent.com/52024566/132992894-d55e1e4b-5833-44cc-bb30-4a1006a840ac.png)

### **JPA는 표준 명세**

- JPA는 인터페이스의 모음
- JPA 2.1 표준 명세를 구현한 3가지 구현체
- 하이버네이트, EclipseLink, DataNucleus

![https://user-images.githubusercontent.com/52024566/132992940-b11dc52d-524e-4897-8b8a-ff101b5af5d4.png](https://user-images.githubusercontent.com/52024566/132992940-b11dc52d-524e-4897-8b8a-ff101b5af5d4.png)

### **JPA를 왜 사용해야 하는가?**

SQL 중심적인 개발에서 **객체 중심으로 개발**할 수 있습니다.

- 생산성
    - 저장: `jpa.persist(member)`
    - 조회: `Member member = jpa.find(memberId)`
    - 수정: `member.setName(“변경할 이름”)`
    - 삭제: `jpa.remove(member)`

- 유지보수
    - 기존에는 필드 변경시 모든 SQL 문을 수정해야 했지만, JPA에서는 필드만 추가하면 SQL은 JPA가 처리합니다.

- 패러다임의 불일치 해결
    1. JPA와 상속 : 특정 객체를 저장할 경우 상속 관계를 JPA가 분석하여 필요한 쿼리를 JPA가 생성합니다.
    2. JPA와 연관관계, JPA와 객체 그래프 탐색 : 지연 로딩을 사용하여 신뢰할 수 있는 엔티티, 계층을 제공합니다.
    3. JPA와 비교하기 : 동일한 트랜잭션에서 조회한 엔티티는 같음을 보장합니다.

- 성능 최적화 기능
    1. 1차 캐시와 동일성(identity) 보장
        1. 같은 트랜잭션 안에서는 같은 엔티티를 반환 - 약간의 조회 성능 향상
        2. DB Isolation Level이 Read Commit이어도 애플리케이션에서 Repeatable Read 보장
        
    2. 트랜잭션을 지원하는 쓰기 지연(transactional write-behind)
        1. 트랜잭션을 커밋할 때까지 INSERT SQL 들을 모은다.
        2. JDBC BATCH SQL 기능을 사용해서 한번에 SQL 전송
        3. UPDATE, DELETE로 인한 로우(ROW)락 시간 최소화
        4. 트랜잭션 커밋 시 UPDATE, DELETE SQL 실행하고, 바로 커밋
        
    3. 지연 로딩(Lazy Loading) 과 즉시 로딩 지원
    
    **지연 로딩**: **객체가 실제 사용될 때 로딩** 
    
    즉시 로딩: JOIN SQL로 한번에 연관된 객체까지 미리 조회
    
    ![https://user-images.githubusercontent.com/52024566/132993188-add758c8-5c57-4be8-ae05-ee5a6b6ea74b.png](https://user-images.githubusercontent.com/52024566/132993188-add758c8-5c57-4be8-ae05-ee5a6b6ea74b.png)
    
- 데이터 접근 추상화와 벤더 독립성
- 표준

**ORM은 객체와 RDB 두 기둥 위에 있는 기술이다.**


# ==== 2. JPA 시작하기 ====
- 프로젝트 생성
- 애플리케이션 개발

## 1. **Hello JPA - 프로젝트 생성**

프로젝트 생성하기 ([https://start.spring.io/](https://start.spring.io/))

- 자바 8 이상(8권장)
- 메이븐 or 그래들 프로젝트
    - groupId: jpa-basic
    - artifactId: ex1-hello-jpa
    - version: 1.0.0
- 사용 라이브러리
    - H2
    - Spring Web
    - Spring boot devtool
    - Spring Data JPA
    - Lombok

![Screenshot 2023-03-01 at 5.31.00 PM.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/31095f70-2191-4d62-9371-deaf169d4ea4/Screenshot_2023-03-01_at_5.31.00_PM.png)

### **H2 데이터베이스**

- 실습용 DB 로 매우 가볍다.(1.5M)
- 웹용 쿼리툴 제공
- MySQL, Oracle 데이터베이스 시뮬레이션 기능
- 시퀀스, AUTO INCREMENT 기능 지원

설정법은 [https://sh1mj1-log.tistory.com/90](https://sh1mj1-log.tistory.com/90) 참고하면 됩니다.

![Screenshot 2023-02-28 at 7.24.30 PM.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/11271a49-0e64-4b00-ae9e-f1262745e0ab/Screenshot_2023-02-28_at_7.24.30_PM.png)

이후에 만들 `persistence.xml` 에서 아래 코드와 같은 이름으로 접속. (`jdbc:h2:tcp://localhost/~/test”`)

```java
<property name="javax.persistence.jdbc.url" value="jdbc:h2:tcp://localhost/~/test"/>
```

### **Maven**

- 자바 라이브러리, 빌드 관리
- 라이브러리 자동 다운로드 및 의존성 관리
- 최근에는 그래들(Gradle)이 점점 유명해지고 있습니다.

### **persistence.xml**

- JPA 설정 파일
- `/META-INF/persistence.xml` 위치
- persistence-unit name으로 이름 지정
- `javax.persistence`로 시작: JPA 표준 속성
- `hibernate`로 시작: 하이버네이트 전용 속성

### **데이터베이스 Dialect (방언)**

![https://user-images.githubusercontent.com/52024566/133097290-f5bdfe83-12dd-4e1f-94d5-913c56fc50f7.png](https://user-images.githubusercontent.com/52024566/133097290-f5bdfe83-12dd-4e1f-94d5-913c56fc50f7.png)

JPA는 특정 데이터베이스에 종속되지 않습니다.

각각의 데이터베이스가 제공하는 SQL 문법과 함수는 조금씩 다릅니다. 아래처럼 말이죠.

- 가변 문자: MySQL은 `VARCHAR`, Oracle은 `VARCHAR2`
- 문자열을 자르는 함수: SQL 표준은 `SUBSTRING()`, Oracle은 `SUBSTR()`
- 페이징: MySQL은 `LIMIT` , Oracle은 `ROWNUM`

Dialect: SQL 표준을 지키지 않는 특정 데이터베이스만의 고유한 기능입니다.

- **hibernate.dialect** 속성에 지정
    - H2 : `org.hibernate.dialect.H2Dialect`
    - Oracle 10g : `org.hibernate.dialect.Oracle10gDialect`
    - MySQL : `org.hibernate.dialect.MySQL5InnoDBDialect`

하이버네이트는 40가지 이상의 데이터베이스 Dialect 을 지원합니다.

## 2. **Hello JPA - 애플리케이션 개발**

### **JPA 구동 방식**

![https://user-images.githubusercontent.com/52024566/133097530-c0572700-aa49-466a-961f-afc73a613367.png](https://user-images.githubusercontent.com/52024566/133097530-c0572700-aa49-466a-961f-afc73a613367.png)

### **객체와 테이블을 생성하고 매핑하기**

```java
@Entity
public class Member {
    
    @Id
    private Long id;
    private String name;
    
    // Getter, Setter
}
```

- `@Entity` : JPA가 관리할 객체라는 것을 알려줌
- `@Id` : DB의 PK와 매핑

실습 하기 전에 h2 데이터베이스에 Member 테이블을 만들어줍니다.

```java
create table Member (
	id bigint not null,
	name varchar(255),
	primary key (id)
)
```

### **실습 - 회원 저장**

**회원 등록**

```java
public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Member member = new Member();
        member.setId(1L);
        member.setName("HelloA");
        em.persist(member);

        tx.commit();

        em.close();

        emf.close();
    }
}
```

`EntityManagerFactory`는 하나만 생성해서 애플리케이션 전체에서 공유합니다.

`EntityManager`는 쓰레드 간에 공유하지 않습니다 (사용하고 버려야 한다).

JPA의 모든 데이터 변경은 트랜잭션 안에서 실행해야 합니다. (`tx.begin()` - `tx.commit()`)

**회원 단일 조회, 삭제, 수정**

```java
public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
            Member findMember = em.find(Member.class, 1L);
            System.out.println("findMember.id = " + findMember.getId());
            System.out.println("findMember.name = " + findMember.getName());

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

        // 회원 삭제
        /*
        try {
            Member findMember = em.find(Member.class, 1L);
            em.remove(findMember);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

         */

        // 회원 수정
        /*
        try {
            Member findMember = em.find(Member.class, 1L);
            findMember.setName("HelloJPA");

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
         */
    }
}
```

트랜잭션에서 문제가 생겼을 경우 트랜잭션을 ROLLBACK 해야 합니다. 또한, 문제 여부와 상관없이 `EntityManager`를 닫아서 DB Connection을 종료해야 합니다.

자바 객체를 수정하듯이 DB를 수정할 수 있습니다. 

JPA를 통하여 객체를 가져올 경우 JPA에서 객체를 관리하여 트랜잭션 시점에 객체의 변경 여부를 감지하여 객체가 변경되었을 경우 UPDATE 쿼리를 생성합니다.

### EntityManager 의 기본적인 CRUD

저장: `persist()`

조회: `find()`

삭제: `remove()`

수정: 수정은 따로 함수를 호출하기 보다는 find 해서 가져온 객체에 setter 메서드를 통해 값을 변경하면 commit() 호출 시 적용되기 전 시스템에서 변경 감지를 통해 기존 객체와 차이점을 찾아서 업데이트를 자동으로 해줍니다.

### 주의점

`EntityManagerFactory` 는 시스템마다 1개만 생성되어서 애플리케이션 전체에서 사용되며 공유됩니다.

EntityManager 는 쓰레드 간에 공유되지 않습니다. 즉, 사용하고 버려야 합니다. 데이터베이스의 커넥션을 공유하지 않는 것과 동일합니다.

### **JPQL 소개**

JPQL 은 식별자를 통한 단순 조회가 아닌 추가 조건들을 통해 조회를 하고자 할 때 사용합니다.

**회원 다수 조회**

```java
public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
			List<Member> result = em.createQuery("select m from Member as m", Member.class).getResultList();
            
			for (Member member : result) {
                System.out.println("member.name = " + member.getName());
            }
            
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
        
        emf.close();
    }
}
```

위에서는 다수 회원들을 조회하고 있습니다.

일반 쿼리와는 다르게 `From` 절의 Member 는 테이블이 아닌 엔티티(`Member.class`) 입니다.  이는 굉장한 장점입니다.

예를 들어서 다수 회원들을 페이징을 한다고 합시다.

```java
List<Member> result = em.createQuery("select m from Member as m", Member.class)
													.setFirstResult(5)
													.setMaxResult(8)
													.getResultList();
```

위 코드처럼 limit 을 8, offset 을 5로 설정하는 것을 메서드를 통해 손쉽게 처리할 수 있습니다. 

그리고 각 DB 의 Dialect(방)언에 따라서 JPA 가 자동으로 맞춰주지요.

또 ansi 가 제공하는 표준 SQL 문법을 모두 제공합니다.

**JPQL 을 정리**하자면 아래와 같습니다.

- JPA를 사용하면 엔티티 객체를 중심으로 개발할 수 있습니다.
    - 문제는 검색 쿼리(JOIN, 집합 통계 쿼리 등)
    - JPA는 검색을 할 때도 테이블이 아닌 엔티티 객체를 대상으로 검색 (테이블에서 가져오면 객체지향 패러다임이 깨짐)
    - 그러나 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능
    - 애플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요함

- JPQL
    - JPA는 SQL을 추상화한 JPQL이라는 객체 지향 쿼리 언어 제공
    - SQL과 문법 유사, SELECT, FROM, WHERE, GROUP BY, HAVING, JOIN 지원
    - JPQL은 엔티티 객체를 대상으로 쿼리
    - SQL은 데이터베이스 테이블을 대상으로 쿼리
    - 테이블이 아닌 객체를 대상으로 검색하는 객체 지향 쿼리
    - SQL을 추상화해서 특정 데이터베이스 SQL에 의존하지 않음
    - JPQL을 한마디로 정의하면 객체 지향 SQL 이다.



# ==== 3. 영속성 관리 - 내부 동작 방식 ==== 
- 영속성 컨텍스트 1
- 플러시
- 준영속 상태

## 1. **영속성 컨텍스트**

JPA에서 가장 중요한 2가지는 아래와 같습니다.

- 객체와 관계형 데이터베이스 매핑하기 (Object Relational Mapping)
    - 정적인 영역입니다.

- 영속성 컨텍스트
    - 동적인 영역입니다.

### **엔티티 매니저 팩토리와 엔티티 매니저**

![https://user-images.githubusercontent.com/52024566/133262752-97bf3b3a-d27a-455c-aa28-876124d547da.png](https://user-images.githubusercontent.com/52024566/133262752-97bf3b3a-d27a-455c-aa28-876124d547da.png)

웹 애플리케이션이 구동하는 시점에 `EntityManagerFactory` 을 생성하여 가지고 있습니다. 

웹 애플리케이션에서 요청이 들어올 경우 `EntityManagerFactory` 는 고객의 요청이 들어올 때마다 `EntityManager`를 생성하고 엔티티 매니저는 DB 커넥션 풀(`Connection Pool`)을 사용하여 DB에 접근, 핸들링합니다.

### **영속성 컨텍스트란**

**영속성 컨텍스트**는 JPA를 이해하는데 가장 중요한 용어로 **엔티티를 영구 저장하는 환경**입니다.

```java
// 엔티티를 영속석 컨텍스트에 저장한다는 코드
EntityManager.persist(entity);
```

영속성 컨텍스트는 논리적인 개념으로 **눈에 보이지 않습니다.** 우리는 엔티티 매니저를 통해서 영속성 컨텍스트에 접근합니다.

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/19014328-b5e0-4502-9507-69517217cb5b/Untitled.png)

### **엔티티의 생명주기**

![https://user-images.githubusercontent.com/52024566/133262753-b1194492-286d-4ca9-b1ea-e905d68f57a0.png](https://user-images.githubusercontent.com/52024566/133262753-b1194492-286d-4ca9-b1ea-e905d68f57a0.png)

- 비영속 (new/transient)
    - 영속성 컨텍스트와 전혀 관계가 없는 새로운 상태

![https://user-images.githubusercontent.com/52024566/133262742-84200f67-0d4c-45a2-8e0d-9ec8292b7ad0.png](https://user-images.githubusercontent.com/52024566/133262742-84200f67-0d4c-45a2-8e0d-9ec8292b7ad0.png)

아래 객체를 생성한 상태는 비영속입니다.

```java
Member member = new Member();
member.setId("member1");
member.setUsername("회원1");
```

- 영속 (managed)
    - 영속성 컨텍스트에 관리되는 상태

![https://user-images.githubusercontent.com/52024566/133262757-2f74ad1a-cbb7-4d5d-ab3e-c4ef7efb565b.png](https://user-images.githubusercontent.com/52024566/133262757-2f74ad1a-cbb7-4d5d-ab3e-c4ef7efb565b.png)

영속 상태가 된다고 해서 쿼리가 날라가는 것이 아닙니다.

```java
EntityManager em = emf.createEntityManager();
em.getTransaction().begin();

//객체를 저장한 상태(영속)
em.persist(member);
```

- 준영속 (detached)
    - 영속성 컨텍스트에 저장되었다가 분리된 상태

```java
//회원 엔티티를 영속성 컨텍스트에서 분리, 준영속 상태
em.detach(member);
```

- 삭제 (removed)
    - 삭제된 상태

```java
// 객체를 삭제한 상태(삭제). 실제로 DB 에서 해당 ROW 을 삭제하는 것.
em.remove(member);
```

### **영속성 컨텍스트의 이점**

영속성 컨텍스트는 **어플리케이션과 DB 사이의 중간 계층**이라고 볼 수 있습니다. 여기서 생기는 이점이 존재합니다.

 **1. 1차 캐시**

![https://user-images.githubusercontent.com/52024566/133262662-fff9c2e3-2cf3-4697-8263-2a7fe2652d9a.png](https://user-images.githubusercontent.com/52024566/133262662-fff9c2e3-2cf3-4697-8263-2a7fe2652d9a.png)

```java
//엔티티를 생성한 상태(비영속)
Member member = new Member();
member.setId("member1");
member.setUsername("회원1");

//1차 캐시에 저장됨
em.persist(member);

//1차 캐시에서 조회
Member findMember = em.find(Member.class, "member1");
```

![https://user-images.githubusercontent.com/52024566/133262732-3d9bc542-bcd1-4f7c-b748-a356b41ffe4f.png](https://user-images.githubusercontent.com/52024566/133262732-3d9bc542-bcd1-4f7c-b748-a356b41ffe4f.png)

JPA에서 조회를 할 경우 바로 DB로 접근하지 않고 영속성 컨텍스트 내의 **1차 캐시에 접근**합니다. 

![https://user-images.githubusercontent.com/52024566/133262736-02b7dd8b-9a31-4120-bd9b-4965c6382b08.png](https://user-images.githubusercontent.com/52024566/133262736-02b7dd8b-9a31-4120-bd9b-4965c6382b08.png)

만약 1차 캐시에 해당 엔티티가 없을 경우 데이터베이스에서 조회합니다.

그러나 엔티티 매니저는 트랜잭션 단위로 존재합니다. 데이터베이스 트랜잭션 내부에서 만들고 종료되기 때문에 하나의 비즈니스 로직이 종료될 경우 1차 캐시는 모두 사라지기 때문에 어플리케이션 전체에서 공유하는 캐시(2차 캐시) 와 달리 성능의 이점은 크지 않다.

물론 비즈니스 로직이 복잡할수록 효과는 클 것입니다.

 **2. 영속 엔티티의 동일성(identity) 보장**

```java
Member a = em.find(Member.class, "member1");
Member b = em.find(Member.class, "member1");
System.out.println(a == b); //동일성 비교 true
```

1차 캐시로 반복 가능한 읽기(REPEATABLE READ) 등급의 트랜잭션 격리 수준을 데이터베이스가 아닌 애플리케이션 차원에서 제공합니다.

즉, JPA 로 엔티티를 조회를 했을 때 같은 엔티티를 조회했다면 동일성을 보장하는 것이지요.

 **3. 트랜잭션을 지원하는 쓰기 지연 (transactional write-behind)**

```java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();

// [트랜잭션] 시작 - 엔티티 매니저는 데이터 변경시 트랜잭션을 시작해야 함
transaction.begin(); // 

em.persist(memberA);
em.persist(memberB);

//여기까지 INSERT SQL을 데이터베이스에 보내지 않는다.
//커밋하는 순간 데이터베이스에 INSERT SQL을 보낸다.
transaction.commit(); // [트랜잭션] 커밋
```

![https://user-images.githubusercontent.com/52024566/133262749-b03a97ed-e013-4faa-838d-1c9a55e3345c.png](https://user-images.githubusercontent.com/52024566/133262749-b03a97ed-e013-4faa-838d-1c9a55e3345c.png)

위 상태에서는 memberA 와 memberB 모두 쓰기 지연 SQL 저장소에 저장되어 있고 실제 DB 에 적용은 안 된 상태입니다.

![https://user-images.githubusercontent.com/52024566/133262746-660da1f2-67a6-4901-9fd4-41f746975eed.png](https://user-images.githubusercontent.com/52024566/133262746-660da1f2-67a6-4901-9fd4-41f746975eed.png)

`commit()` 시점에 쓰기 지연 SQL 에 저장된 쿼리들을 다 실행시켜서 DB 에 적용합니다.

이렇게 쓰기 지연을 사용하면 버퍼링 기능을 가질 수 있습니다. 쿼리를 매 순간 여러번 날리지 않고 최적화가 가능하지요.

```java
<property name="hibernate.jdbc.batch_size" value = "10"/>
```

위처럼 몇개릴 버퍼링 할지 설정할 수 있습니다.

 **4. 변경 감지(Dirty Checking)**

```java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();
transaction.begin(); // [트랜잭션] 시작

// 영속 엔티티 조회
Member memberA = em.find(Member.class, "memberA");

// 영속 엔티티 데이터 수정
memberA.setUsername("hi");
memberA.setAge(10);

//em.update(member) 이런 코드가 있어야 하지 않을까?

transaction.commit(); // [트랜잭션] 커밋
```

![https://user-images.githubusercontent.com/52024566/133262740-10892bdd-d6fc-4f82-b79a-23bd3fc51f1f.png](https://user-images.githubusercontent.com/52024566/133262740-10892bdd-d6fc-4f82-b79a-23bd3fc51f1f.png)

1차 캐시안에는 `@Id`, `Entity`, `스냅샷`이 있습니다. 최초로 영속성 컨텍스트(1차 캐시)에 들어오는 순간 스냅샷을 찍어서 저장해둡니다.

JPA 는 프랜잭션이 `commit()`되는 순간에 엔티티와 스냅샷을 모두 비교합니다. 만약 변경된 것이 있을 경우 쓰기 지연 SQL 저장소에 업데이트 쿼리를 저장하고 수행하게 됩니다. 

즉, 따로 `em.update(member);`  와 같은 코드가 전혀 필요하지 않는 것입니다.

이러한 장점 외에도 영속성 컨텍스트를 사용하면 지연 로딩 같은 다른 장점들도 가지고 있습니다.

## 2. **플러시**

플러시는 영속성 컨텍스트의 변경내용을 데이터베이스에 반영, 동기화(sync)하는 것을 말합니다. 

### 플러시 발생

만약 **플러시가 발생**하면 아래와 같은 프로세스가 이루어집니다.

1. 변경 감지
2. 수정된 엔티티 쓰기 지연 SQL 저장소에 등록
3. 쓰기 지연 SQL 저장소의 쿼리를 데이터베이스에 전송 (등록, 수정, 삭제 쿼리)

### 플러시 방법

영속성 컨텍스트는 자동으로 호출할 수 있고 수동으로 플러시할 수도 있습니다.

- `em.flush()` - 직접 호출. 일반적인 경우에는 사용하지 않으나 테스트할 때 사용할 수 있음
- 트랜잭션 커밋 - 플러시 자동 호출
- JPQL 쿼리 실행 - 플러시 자동 호출

**JPQL 쿼리 실행시 플러시가 자동으로 호출되는 이유**

```java
em.persist(memberA);
em.persist(memberB);
em.persist(memberC);

//중간에 JPQL 실행
query = em.createQuery("select m from Member m", Member.class);
List<Member> members= query.getResultList();
```

JPQL 쿼리를 실행하는 시점 바로 이전에 member 셋을 영속성 컨텍스트에 넣었습니다. 

JPQL 쿼리를 실행하는 시점에 이 member 들이 조회가 안되는 경우를 막기 위해 JPA 에서는 JPQL 쿼리를 수행하기 전에 flush 을 실행하여 DB 와 영속성 컨텍스트 간에 동기화를 해줍니다.

**플러시는 영속성 컨텍스트를 비우지 않습니다.**

단지 영속성 컨텍스트의 변경 내용을 데이터베이스에 동기화하는 작업을 합니다. 

우리는 커밋 직전에만 동기화하면 되므로 트랜잭션이라는 작업 단위가 중요합니다!

**플러시 모드 옵션**

```java
em.setFlushMode(FlushModeType.COMMIT)
```

- FlushModeType.AUTO : 커밋이나 쿼리를 실행할 때 플러시 (기본값, 변경할 일은 거의 없다)
- FlushModeType.COMMIT : 커밋할 때만 플러시

## 3. **준영속 상태**

위에서 알아보았던 준영속 상태를 조금 더 생각해봅시다.

**준영속 상태**는 **영속 상태의 엔티티가 영속성 컨텍스트에서 분리(detached)** 된 것이라고 했습니다.

그렇다면 당연히 **영속성 컨텍스트가 제공하는 기능을 사용할 수 없습니다.**

### **준영속 상태로 만드는 방법**

```java
// 특정 엔티티만 준영속 상태로 전환
em.detach(entity)

// 영속성 컨텍스트를 완전히 초기화
em.clear()

// 영속성 컨텍스트를 종료
em.close()
```

# ==== 4. 엔티티 매핑 ====
- 객체와 테이블 매핑
- 데이터베이스 스키마 자동 생성
- 필드와 칼럼 매핑
- 기본 키 매핑
- 예제 - 요구사항 분석과 기본 매핑

## 1. **객체와 테이블 매핑**

JPA 을 사용하려면 객체와 테이블을 잘 설계하고 매핑하는 것이 가장 중요합니다. 

### **@Entity**

`@Entity`가 붙은 클래스는 말 그대로 엔티티가 되며 JPA가 관리합니다.

JPA를 사용해서 테이블과 매핑할 클래스는 `@Entity` 애노테이션이 필수로 있어야 합니다.

`@Entity` 클래스를 사용할 때는 몇가지 **주의사항**이 있습니다.

- 기본 생성자 필수
- `final` 클래스, `enum`, `interface`, `inner` 클래스 사용 X
- 저장할 필드에 `final` 사용 X

`**@Entity` 속성** 정리

`name` ( `@Entity(name = "entityName" )` )

- JPA에서 사용할 엔티티 이름을 지정합니다.
- 기본값은 클래스 이름을 그대로 사용합니다.(예: Member)
- 대부분 같은 클래스 이름이 없으면 가급적 기본값을 사용합니다.

### @Table

`@Table`은 애노테이션으로 엔티티와 매핑할 테이블을 지정할 수 있습니다.

**`@Table` 속성 정리**

- name : 매핑할 테이블 이름. (기본값으로는 엔티티 이름)
- catalog: 데이터베이스 catalog 매핑
- schema: 데이터베이스 schema 매핑
- uniqueContraints: DDL 생성 시에 unique 제약 조건을 생성

## 2. **데이터베이스 스키마 자동 생성**

우리는 개발을 하면서 데이터베이스 테스트를 위해 스키마(DB 테이블)을 만들고 데이터를 직접 집어넣고 데이터를 삭제하고 다시 스키마(DB 테이블)를 DROP 하고 다시 만들고.. .이러한 것들을 계속 반복하게 될 것입니다. 이런 개발 과정을 간편하게 만들어 주는 방법이 존재합니다! 

JPA 을 이용하여 **DDL(데이터 정의어)**을 애플리케이션 실행 시점에 **자동 생성**할 수 있습니다. 

이로써 테이블 중심 개발 →  **객체 중심 개발**  을 가능하게 합니다.

데이터베이스 dialect을 활용해서 데이터베이스에 맞는 적절한 DDL을 자동으로 생성합니다.

**이렇게 생성된 DDL은 개발 장비(테스트)에서만 사용해야 합니다.**

- 보통 생성된 DDL은 운영서버에서는 바로 사용하지 않거나, 적절히 다듬은 후 사용합니다.

데이터베이스 스키마를 자동 생성할 때 여러 속성을 지정할 수 있습니다.

![Screenshot 2023-03-02 at 11.30.24 AM.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/14295103-fe60-4d90-99ce-941e517cb141/Screenshot_2023-03-02_at_11.30.24_AM.png)

- create: 기존 테이블 삭제 후 다시 생성(DROP + CREATE)
- create-drop: create 와 같으나 종료 시점에 테이블 DROP
- update: 변경부분만 반영(운영 DB 에는 사용하면 안됨)
- validate: 엔티티와 테이블이 정상 매핑되었는지만 확인
- none: 자동 생성을 하지 않음. (관례 상 none 이라고 적지만 위 속성이 아닌 것을 아무거나 적어도 똑같습니다.)

### **주의**

**운영 장비(실제 서버)에는 절대 create, create-drop, update 사용하면 안 됩니다.**

개발 초기 단계는 `create` 또는 `update` 속성을 사용하고

테스트 서버는 `update` 또는 `validate` 속성,

스테이징과 운영 서버는 `validate` 또는 `none` 을 사용하는 것이 보통입니다.

### **DDL 생성 기능**

데이터베이스 스키마 자동 생성 기능을 사용할 때 제약 조건을 가진 DDL 을 만들 수 있습니다.

**제약조건 추가**

예를 들어 회원 이름은 필수, 10자 초과 불가 등의 조건을 만들 수 있습니다. 

```java
@Column(nullable = false, length = 10)
```

**유니크 제약조건 추가**

```java
@Table(uniqueConstraints = 
			{@UniqueConstraint( 
								name = "NAME_AGE_UNIQUE", 
								columnNames = {"NAME", "AGE"} )})
```

DDL 생성 기능은 DDL을 자동 생성할 때만 사용되고 JPA의 실행 로직에는 영향을 주지 않습니다.

## 3. **필드와 컬럼 매핑**

`Member` 클래스를 아래와 같은 요구사항에 맞춰서 수정해봅시다.

1. 회원은 일반 회원과 관리자로 구분해야 한다.
2. 회원 가입일과 수정일이 있어야 한다.
3. 회원을 설명할 수 있는 필드가 있어야 한다. 이 필드는 길이 제한이 없다.

```java
@Entity
public class Member {
    
    @Id
    private Long id;
    
    @Column(name = "name")
    private String username;
    
    private Integer age;
    
    @Enumerated(EnumType.STRING)
    private RoleType roleType; // USER, ADMIN 을 갖는 enum 클래스를 별도로 생성함.
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;
    
    @Lob
    private String description;
    //Getter, Setter…
}
```

### **매핑 어노테이션 정리**

**`@Column` - 가장 자주 쓰임**

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| name | 필드와 매핑할 테이블의 컬럼 이름 | 객체의 필드 이름 |
| insertable, updatable | 등록, 변경 가능 여부 | TRUE |
| nullable(DDL) | null 값의 허용 여부를 설정한다.
false로 설정하면 DDL 생성 시에 not null 제약조건이 붙는다. | TRUE |
| unique(DDL) | @Table의 uniqueConstraints와 같지만 한 컬럼에 간단히 유니크 제약조건을 걸 때 사용한다. | FALSE |
| columnDefinition(DDL) | 데이터베이스 컬럼 정보를 직접 줄 수 있다.
ex) “varchar(100)“ | “” (공백) |
| length(DDL) | 문자 길이 제약조건, String 타입에만 사용한다. | 255 |
| precision, scale(DDL) | BigDecimal 타입에서 사용(BigInteger도).

precision은 소수점을 포함한 전체 자릿수를, 
scale은 소수의 자릿수다.

double, float 타입에는 적용되지 않는다. 아주 큰 숫자나 정밀한 소수를 다루어야 할 때만 사용한다. | precision=19, 
scale=2 |

**`@Enumerated`**

자바 enum 타입을 매핑할 때 사용합니다.

- STRING: ENUM 이름을 데이터베이스에 저장
- ORDINAL: ENUM 순서를 데이터베이스에 저장 (기본값)

ORDINAL 을 사용할 경우 **객체가 변경되었을 때 큰 문제가 생길 수 있으므로 사용하지 않는다** 

- 예를 들어서 enum 타입이 추가, 변경, 삭제되어 순서가 달라지면 문제가 생깁니다.

**`@Temporal`**

날짜 타입(java.util.Date, java.util.Calendar)을 매핑할 때 사용합니다.

- DATE: 날짜, 데이터베이스 date 타입과 매핑.
- TIME: 시간, 데이터베이스 time 타입과 매핑
- TIMESTAMP: 날짜와 시간, 데이터베이스 timestamp 타입과 매핑.

`LocalDate`, `LocalDateTime`을 사용할 때는 생략이 가능합니다. (최신 하이버네이트 지원)

**`@Lob`**

데이터베이스 BLOB, CLOB 타입과 매핑할 때 사용합니다.

@Lob에는 지정할 수 있는 속성이 따로 없으며 매핑하는 필드 타입이 문자면 CLOB 매핑, 나머지는 BLOB 매핑합니다.

- CLOB: `String`, `char[]`, `java.sql.CLOB`
- BLOB: `byte[]`, `java.sql.BLOB`

**`@Transient`**

필드 매핑이 되지 않도록 하는 어노테이션입니다.

데이터베이스에 저장되지 않고 조회도 할 수 없습니다. 주로 **메모리상에서만 임시로 어떤 값을 보관하고 싶을 때** 사용합니다.

## 4. **기본 키 매핑**

기본키는 자바 코드를 통해 하나하나 **직접 할당**을 할 수도 있고 **자동 생성**되도록 할 수도 있습니다.

직접 할당: `@Id`만 사용

**자동 생성(`@GeneratedValue`)** - strategy 속성을 지정할 수 있습니다.

- IDENTITY: 데이터베이스에 위임.
- SEQUENCE: 데이터베이스 시퀀스 오브젝트 사용
- TABLE: 키 생성용 테이블 사용, 모든 DB에서 사용
    - @TableGenerator 필요

- AUTO: dialect에 따라 자동 지정, 기본값

```java
@Id @GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

AUOTO 는 단순하므로 IDENTITY, SEQUENCE, TABLE 전략을 구체적으로 알아봅시다.

### **IDENTITY 전략**

위에 설명처럼 기본 키 생성을 데이터베이스에 위임하는 것입니다.

주로 MySQL, PostgreSQL, SQL Server, DB2에서 사용합니다. (ex:  MySQL의 AUTO_ INCREMENT)

JPA는 보통 트랜잭션 커밋 시점에 INSERT SQL 실행하므로 **AUTO_ INCREMENT 는 데이터베이스에 INSERT SQL을 실행한 이후에 ID 값을 알 수 있습니다.**

즉, 영속성 관리 시점에서 1차 캐시에 `@Id` 값을 알 수 없어지기 때문에 이 경우 `em.persist()` 수행 시 바로 쿼리가 수행됩니다. 

결론적으로 IDENTITY 케이스 에서는 **지연쓰기가 제한**됩니다.

### **SEQUENCE 전략**

데이터베이스 시퀀스는 유일한 값을 순서대로 생성하는 특별한 데이터베이스 오브젝트 입니다. `@SequenceGenerator` 가 필요합니다. (ex: 오라클 시퀀스)

오라클, PostgreSQL, DB2, H2 데이터베이스에서 사용합니다.

영속화(`em.persist()`) 시에 시퀀스에서 next value 을 가져와서  `@Id` 가 해당 값을  가지고 1차 캐싱을 해줍니다.

이 경우 지연 쓰기가 가능합니다.

```java
@Entity
@SequenceGenerator(
    name = “MEMBER_SEQ_GENERATOR",
    sequenceName = “MEMBER_SEQ", //매핑할 데이터베이스 시퀀스 이름
    initialValue = 1, allocationSize = 1)

public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
```

**`@SequenceGenerator` - 속성**

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| name | 식별자 생성기 이름 (필수) |  |
| sequenceName | 데이터베이스에 등록되어 있는 시퀀스 이름 | hibernate_sequence |
| initialValue | DDL 생성 시에만 사용됨.
시퀀스 DDL을 생성할 때 처음 시작하는 수를 지정한다. | 1 |
| allocationSize | 시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨)
데이터베이스 시퀀스 값이 하나씩 증가하도록 설정되어 있으면 이 값을 반드시 1로 설정해야 한다 | 50 |
| catalog, schema | 데이터베이스 catalog, schema 이름 |  |

SEQUENCE 전략에 더 자세한 것은 이 블로그에서 자세히 설명되어 있네요!

[https://dololak.tistory.com/479](https://dololak.tistory.com/479)

### **TABLE 전략**

**키 생성 전용 테이블**을 하나 만들어서 **데이터베이스 시퀀스를 흉내**내는 전략입니다.

- 장점: 모든 데이터베이스에 적용 가능
- 단점: 성능이 떨어짐.

```java
@Entity
@TableGenerator(
    name = "MEMBER_SEQ_GENERATOR",
    table = "MY_SEQUENCES",
    pkColumnValue = “MEMBER_SEQ", allocationSize = 1)

public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,
                    generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
}
```

데이터베이스 키 전용 테이블을 아래처럼 만들고 애플리케이션을 실행해보면 아래와 같이 MY_SEQUENCES 에 데이터가 쌓입니다.

```sql
create table MY_SEQUENCES (
    sequence_name varchar(255) not null,
    next_val bigint,
    primary key ( sequence_name )
)
```

```sql
// MY_SEQUENCES 테이블 조회
SELECT * FROM MY_SEQUENCES
```

| SEQUENCE_NAME | NEXT_VAL |
| --- | --- |
| MEMBER_SEQ | 1 |

**`@TableGenerator` - 속성**

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| name | 식별자 생성기 이름(필수) |  |
| table | 키생성 테이블명
(ex: MEMBER_SEQ_GENERATOR) | hibernate_sequences |
| pkColumnName | 시퀀스 컬럼명
(ex: MY_SEQUENCES) | sequence_name |
| valueColumnName | 시퀀스 값 컬럼명 | next_val |
| pkColumnValue | 키로 사용할 값 이름 | 엔티티 이름 |
| initialValue | 초기 값, 마지막으로 생성된 값이 기준이다. | 0 |
| allocationSize | 시퀀스 한 번 호출에 증가하는 수
(성능 최적화에 사용됨) | 50 |
| catalog, schema | 데이터베이스 catalog, schema 이름 |  |
| uniqueConstraints(DDL) | 유니크 제약 조건을 지정할 수 있다. |  |

### **권장하는 식별자 전략**

기본 키 제약 조건: `not null`, `unique`, `not update`

보통 위 조건을 계속 만족하는 자연키는 찾기 힘들기 때문에 비즈니스와 연관이 없는 대리키(대체키)를 사용합니다. 

예를 들어 자연키인 주민등록번호는 변하지 않는다고 기본키로 선택해도 괜찮을 거라고 생각할지도 모릅니다. 하지만 이런 상황이 생길 수도 있죠. 회원 테이블의 PK 로 주민등록번호를 사용했을 때 연관 매핑을 맺은 다른 테이블에서도 외래키로 주민등록번호를 사용하기 때문에 개인정보가 매우 퍼지게 될 수 있습니다.

그러므로 **아래 전략을 사용하는 것을 가장 추천**합니다.

**Long형 + 대체키 + 키 생성전략 사용 (AUTO 혹은 SequenceObject + 회사나 프로젝트의 규칙)**

- 엔티티를 처음 new로 생성하는 시점에는 값이 없으므로 null인 상태가 필요합니다. 기본형 int는 null 값을 가질 수 없으므로 참조형이면서 Integer 보다 크기가 큰 Long을 사용합니다.

### **SEQUENCE 전략과 최적화**

기본키 생성 전략이 SEQUENCE 인 경우에 잦은 네트워크 통신 문제가 일어날 수 있습니다.

IDENTITY 전략은 데이터베이스에서 INSERT SQL 문이 실행된 이후에 ID 을 알 수 있는 반면, SEQUENCE 전략은 `em.persist()` 하는 시점에서 데이터베이스에서 ID 만 따로 조회해올 수 있습니다.

하지만 `em.persist()` 를 할 때 네트워크를 이용하게 되어서 성능 이슈가 발생할 수도 있습니다. 시퀀스를 얻기 위해 네트워크 통신을 한번 하고, 엔티티에 대한 작업을 할 때 다시 한번 더 네트워크 통신이 일어나는 것이죠.

이 경우에는 하이버네이트의 `allocationSize` 라는 옵션을 사용할 수 있습니다. 이 옵션으로 시퀀스를 한 번에 여러 개 호출할 수 있습니다. 이 옵션은 동시성 문제도 없습니다.

## 5. **예제 - 1. 요구사항 분석과 기본 매핑**

이제 예제를 통해 직접 기본적인 객체-테이블 매핑을 해봅시다. 

요구사항은 아래와 같습니다.

- 요구사항 분석
    - 회원은 상품을 주문할 수 있다.
    - 주문 시 여러 종류의 상품을 선택할 수 있다.

기능

- 회원 기능
    - 회원 등록
    - 회원 조회

- 상품 기능
    - 상품 등록
    - 상품 수정
    - 상품 조회

- 주문 기능
    - 상품 주문
    - 주문내역 조회
    - 주문 취소

### **도메인 모델 분석**

![https://user-images.githubusercontent.com/52024566/133449160-c9292286-ace7-4ac7-9a9c-8a242cf60029.png](https://user-images.githubusercontent.com/52024566/133449160-c9292286-ace7-4ac7-9a9c-8a242cf60029.png)

회원과 주문의 관계: 회원은 여러 번 주문할 수 있습니다. ( 1:N )

주문과 상품의 관계: 주문할 때 여러 상품을 선택할 수 있습니다. 반대로 같은 상품도 여러 번 주문될 수 있습니다. 

[주문 - 상품] 은 [N:M] 이고 주문상품 이라는 모델을 만들어서 다대다 관계를 일다대, 다대일 관계로 풀어냈습니다.

### **테이블 설계**

![https://user-images.githubusercontent.com/52024566/133449171-93576449-f0f9-4d23-8059-ac39016bf500.png](https://user-images.githubusercontent.com/52024566/133449171-93576449-f0f9-4d23-8059-ac39016bf500.png)

DB에서 ORDER가 예약어로 걸려 있는 경우가 많아서 테이블명이 ORDER일 경우 오류가 발생할 수도 있어 ORDERS 로 테이블을 만들었습니다.

### **엔티티 설계와 매핑**

![https://user-images.githubusercontent.com/52024566/133449169-6b411c55-0ced-4edc-87e6-59fdc5060a8e.png](https://user-images.githubusercontent.com/52024566/133449169-6b411c55-0ced-4edc-87e6-59fdc5060a8e.png)

설계가 이뻐보이지만 사실 이 설계는 문제가 있습니다.

### **데이터 중심 설계의 문제점**

현재 방식은 객체 설계를 테이블 설계에 맞춘 방식입니다. 테이블의 외래키를 객체에 그대로 가져온 것입니다.

이렇게 되면 **객체 그래프 탐색이 불가능**하고, 서로 간의 **참조가 없어서 UML 도 잘못되어있습**니다.

예를 들어서 특정 주문을 한 회원에 대한 정보를 조회한다고 합시다.

```java
Order order = em.find(Order.class, 1L);
Long memberId = order.getMemberId();

Member orderedMember = em.find(Member.class, memberId);
```

ORDERS 테이블과 MEMBER 테이블 간의 참조가 없어서 주문한 회원의 `memberId` 을 `EntityManager` 을 통해 다시 탐색하고 있습니다. 

이러한 방식보다는 아래처럼 `member` 정보를 가져오는 것이 훨씬 더 객체지향적입니다.

```java
Member orderMember = order.getMember();
```

