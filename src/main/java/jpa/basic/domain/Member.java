package jpa.basic.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;
    private String name;
    private String city;
    private String street;
    private String zipcode;

    /* Member 가 orders 을 가지고 있는 것은 좋은 설계가 아님.
    Order 에 이미 memberId 가 FK 로 있기 때문에
    고객의 주문 내역을 조회한다면 Order 에서 memberId 을 가지고 조회하는 것이 더 깔끔함.
     */
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

}
