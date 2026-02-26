package ch.bbw.m323.streams;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.bbw.m323.streams.PersonStreamTest.Person.Country;
import ch.bbw.m323.streams.PersonStreamTest.Person.Gender;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

class PersonStreamTest implements WithAssertions {

	record Person(String name, int age, Gender gender, Country country) {

		enum Gender {
			MALE, FEMALE, NON_BINARY
		}

		record Country(String name, long population) {
		}

		public boolean isAdult() {
			return age >= 18;
		}
	}

	final Country france = new Country("France", 65_235_184L);

	final Country canada = new Country("Canada", 37_653_095L);

	final Country uk = new Country("United Kingdom", 67_791_734L);

	final List<Person> people = List.of(
			new Person("Brent", 50, Gender.MALE, canada),
			new Person("Luca", 22, Gender.MALE, canada),
			new Person("May", 12, Gender.FEMALE, france),
			new Person("Jojo", 23, Gender.NON_BINARY, uk),
			new Person("Maurice", 15, Gender.MALE, france),
			new Person("Alice", 15, Gender.FEMALE, france),
			new Person("Laurence", 22, Gender.MALE, france),
			new Person("Samantha", 67, Gender.FEMALE, canada));

	// tag::sample[]
	@Test
	void allNamesUppercase() { // Alle Namen UPPERCASE.
		// Dies ist eine Beispielimplementation, wie eine Lösung auszusehen hat.
		// Die Spielregel wurde eingehalten: nur ein `;` am Ende der Funktion
		assertThat(people.stream() // ein Stream<Person>
				.map(Person::name) // ein Stream<String> mit allen Namen. Dasselbe wie `.map(x -> x,name())`.
				.map(String::toUpperCase) // ein Stream<String> mit UPPERCASE-Namen
				.toList() // eine List<String>
		).containsExactly("BRENT", "LUCA", "MAY", "JOJO", "MAURICE", "ALICE", "LAURENCE", "SAMANTHA");
	}
	// end::sample[]

	// TODO: add all your own Testcases here

	@Test
	void max4Letters() {
		assertThat(people.stream()
				.map(Person::name)
				.filter(name -> name.length() <= 4)
				.toList()
		).containsOnly("Luca", "May", "Jojo");
	}

	@Test
	void sumAgePersons() {
		assertThat(people.stream()
				.mapToInt(Person::age)
				.sum()
		).isEqualTo(226);
	}

	@Test
	void oldestAge() {
		assertThat(people.stream()
				.mapToInt(Person::age)
				.max()
				.orElseThrow()
		).isEqualTo(67);
	}

	@Test
	void allKanadischeMans() {
		assertThat(people.stream()
				.filter(p -> p.country().equals(canada))
				.filter(p -> p.gender() == Gender.MALE)
				.toList()
		).hasSize(2).allSatisfy(x -> assertThat(x).isInstanceOf(Person.class));
	}

	@Test
	void nameWithUnderline() {
		assertThat(people.stream()
				.map(Person::name)
				.collect(Collectors.joining("_"))

		).hasSize(51).contains("_");
	}

	@Test
	void womanFromSmallCountries() {
		assertThat(people.stream()
				.filter(p -> p.gender() == Gender.FEMALE)
				.filter(p -> p.country().population() <= 1_00_00)
				.toList()
		).isEmpty();
	}

	@Test
	void maleNamesSortedByAge() {
		assertThat(people.stream()
				.filter(p -> p.gender() == Gender.MALE)
				.sorted(Comparator.comparingInt(Person::age))
				.map(Person::name)
				.toList()
		).containsExactly("Maurice", "Luca", "Laurence", "Brent");
	}

	@Test
	void secondOldestWoman() {
		assertThat(people.stream()
				.filter(p -> p.gender() == Gender.FEMALE)
				.sorted(Comparator.comparingInt(Person::age).reversed())
				.limit(2)
				.skip(1)
				.findFirst()
				.orElseThrow()
		).extracting(Person::name).isEqualTo("Alice");
	}

	@Test
	void evenNumbers0to100NotAPersonAge() {
		final var ages = people.stream()
				.map(Person::age)
				.collect(Collectors.toSet());

		assertThat(IntStream.range(0, 100)
				.filter(n -> n % 2 == 0)
				.filter(n -> !ages.contains(n))
				.boxed()
				.toList()
		).hasSize(47).contains(0, 62, 98).doesNotContain(22);
	}

	@Test
	void allLettersOfAllNamesSortedAlphabetically_flatMap() {
		assertThat(people.stream()
				.map(Person::name)
				.flatMap(name -> name.chars().mapToObj(c -> (char) c))
				.distinct()
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining())
		).isEqualTo("ABJLMSacehijlmnortuy");
	}

	@Test
	void allNamesSortedAscThenDesc_inOneList() {
		assertThat(
				java.util.stream.Stream.concat(
						people.stream().map(Person::name).sorted(),
						people.stream().map(Person::name).sorted(Comparator.reverseOrder())
				).toList()
		).hasSize(people.size() * 2).startsWith("Alice").endsWith("Alice");
	}

	@Test
	void youngestPersonOfEachCountry_groupingBy() {
		final Map<Country, Optional<Person>> youngestByCountry = people.stream()
				.collect(Collectors.groupingBy(
						Person::country,
						Collectors.minBy(Comparator.comparingInt(Person::age))
				));

		assertThat(youngestByCountry.values().stream()
				.map(Optional::orElseThrow)
				.toList()
		).extracting(Person::name).containsOnly("Jojo", "Luca", "May");
	}

	@Test
	void secondOldestWoman_assumingOnlyOne() {
		assertThat(people.stream()
				.filter(p -> p.gender() == Gender.FEMALE)
				.sorted(Comparator.comparingInt(Person::age).reversed())
				.skip(1)
				.findFirst()
				.orElseThrow()
		).extracting(Person::name).isEqualTo("Alice");
	}

	@Test
	void namesOfPeopleWhoShareAgeWithAnotherPerson() {
		final var duplicateAges = people.stream()
				.collect(Collectors.groupingBy(Person::age, Collectors.counting()))
				.entrySet().stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		assertThat(people.stream()
				.filter(p -> duplicateAges.contains(p.age()))
				.toList()
		).extracting(Person::name).containsOnly("Luca", "Maurice", "Alice", "Laurence");
	}

	@Test
	void countriesWithMoreThanThreeAdults() {
		assertThat(people.stream()
				.collect(Collectors.groupingBy(Person::country, Collectors.counting()))
				.entrySet().stream()
				.filter(e -> e.getValue() > 3)
				.map(Map.Entry::getKey)
				.toList()
		).contains(france);
	}

	@Test
	void menWhoFollowAfterAWomanInListOrder() {
		assertThat(IntStream.range(1, people.size())
				.filter(i -> people.get(i - 1).gender() == Gender.FEMALE)
				.mapToObj(people::get)
				.filter(p -> p.gender() != Gender.FEMALE)
				.toList()
		).extracting(Person::name).containsOnly("Jojo", "Laurence");
	}

}
