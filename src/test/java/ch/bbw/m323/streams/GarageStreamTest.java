package ch.bbw.m323.streams;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GarageStreamTest implements WithAssertions {

	Inventory inventory;

	record Inventory(List<Customer> products) {

		record Customer(String id, String customer, String email, List<Car> cars) {

			record Car(String brand, String price, Wheel wheels, Radio radio) {

				record Wheel(String brand, Integer amount) {}

				record Radio(Boolean ukw, Bluetooth bluetooth) {

					record Bluetooth(Integer version, List<Standard> standards) {

						record Standard(String codec, Boolean partial) {}
					}
				}
			}
		}
	}

	private static <T> Stream<T> safeStream(List<T> list) {
		return list == null ? Stream.empty() : list.stream();
	}

	private Stream<Inventory.Customer> customers() {
		return inventory == null ? Stream.empty() : safeStream(inventory.products());
	}

	private static Stream<Inventory.Customer.Car> carsOf(Inventory.Customer c) {
		return c == null ? Stream.empty() : safeStream(c.cars());
	}

	private Stream<Inventory.Customer.Car> allCars() {
		return customers().flatMap(GarageStreamTest::carsOf);
	}

	@BeforeEach
	void readJson() throws IOException {
		// TODO: change to "manynull.json" for a harder experience
		try (var in = GarageStreamTest.class.getClassLoader().getResourceAsStream("fewnull.json")) {
			inventory = new ObjectMapper().readValue(in, Inventory.class);
		}
	}

	@Test
	void customerNamesWithTwoOrMoreCars() {

		Predicate<Inventory.Customer> hasAtLeastTwoCars =
			c -> c != null && c.cars() != null && c.cars().size() >= 2;
		assertThat(
			customers()
					.filter(hasAtLeastTwoCars)
					.map(Inventory.Customer::customer)
					.filter(Objects::nonNull)
					.toList()
		).hasSizeBetween(10, 11);

	}

	@Test
	void countCarsWithUkwRadio(){

		Predicate<Inventory.Customer.Car> hasUkw =
				car -> car !=null
				&& car.radio() != null
				&& Boolean.TRUE.equals(car.radio().ukw());

		assertThat(
				allCars()
						.filter(hasUkw)
						.count()
		).isIn(8L, 16L);
	}
}
