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

