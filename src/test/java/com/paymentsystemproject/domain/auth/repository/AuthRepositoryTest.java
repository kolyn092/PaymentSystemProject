package com.paymentsystemproject.domain.auth.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import com.paymentsystemproject.domain.member.entity.Member;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:authdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthRepositoryTest {

	@Autowired
	private AuthRepository authRepository;

	@Test
	@DisplayName("회원 엔티티를 저장한다")
	void save_success() {
		Member member = member();

		Member savedMember = authRepository.save(member);

		assertThat(savedMember.getId()).isNotNull();
		assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
		assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
		assertThat(savedMember.getName()).isEqualTo("User Name");
		assertThat(savedMember.getPhone()).isEqualTo("010-1234-5678");
		assertThat(savedMember.getPointBalance()).isZero();
	}

	@Test
	@DisplayName("이메일이 존재하면 회원을 조회한다")
	void findByEmail_success() {
		authRepository.save(member());

		Optional<Member> foundMember = authRepository.findByEmail("user@example.com");

		assertThat(foundMember).isPresent();
		assertThat(foundMember.get().getEmail()).isEqualTo("user@example.com");
		assertThat(foundMember.get().getName()).isEqualTo("User Name");
		assertThat(foundMember.get().getPhone()).isEqualTo("010-1234-5678");
	}

	@Test
	@DisplayName("이메일이 존재하지 않으면 빈 Optional을 반환한다")
	void findByEmail_empty() {
		Optional<Member> foundMember = authRepository.findByEmail("missing@example.com");

		assertThat(foundMember).isEmpty();
	}

	private Member member() {
		return new Member("user@example.com", "encoded-password", "User Name", "010-1234-5678");
	}
}
