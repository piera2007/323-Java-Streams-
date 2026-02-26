package ch.bbw.m323.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class GarageStreamAdvancedTest implements WithAssertions {

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
		return customers().flatMap(GarageStreamAdvancedTest::carsOf);
	}

	private static int parsePrice(String price) {
		if (price == null) return Integer.MAX_VALUE;
		String digits = price.replaceAll("[^0-9]", "");
		return digits.isBlank() ? Integer.MAX_VALUE :Integer.parseInt(digits);
	}

	@BeforeEach
	void readJson() throws IOException {
		// TODO: change to "manynull.json" for a harder experience
		try (var in = GarageStreamAdvancedTest.class.getClassLoader().getResourceAsStream("manynull.json")) {
			inventory = new ObjectMapper().readValue(in, Inventory.class);
		}
	}

	@Test
	void existsCheapCarWithBluetooth5() {
		assertThat(
				allCars()
						.filter(Objects::nonNull)
						.anyMatch(car ->
								parsePrice(car.price()) < 20_000
						&& car.radio() != null
						&& car.radio().bluetooth != null
						&& Integer.valueOf(5).equals(car.radio().bluetooth().version())
				)
		).isTrue();
	}

	@Test
	void wheelBrandsAndHowManyWheelsSold() {
		Map<String, Integer> wheelsPerBrand =
				allCars()
						.filter(Objects::nonNull)
						.map(Inventory.Customer.Car::wheels)
						.filter(Objects::nonNull)
						.filter(w -> w.brand() != null)
						.collect(Collectors.groupingBy(
								Inventory.Customer.Car.Wheel::brand,
								Collectors.summingInt(w -> w.amount() == null ? 0 : w.amount())
						));
		assertThat(wheelsPerBrand).isNotEmpty();
	}

	@Test
	void customerNamesWithOpusCodec() {
		assertThat(
				customers()
						.filter(Objects::nonNull)
						.filter(c ->
								carsOf(c)
										.filter(Objects::nonNull)
										.map(Inventory.Customer.Car::radio)
										.filter(Objects::nonNull)
										.map(Inventory.Customer.Car.Radio::bluetooth)
										.filter(Objects::nonNull)
										.flatMap(bt -> safeStream(bt.standards()))
										.filter(Objects::nonNull)
										.map(Inventory.Customer.Car.Radio.Bluetooth.Standard::codec)
										.anyMatch("Opus"::equals)
						)
						.map(Inventory.Customer::customer)
						.filter(Objects::nonNull)
						.distinct()
						.toList()
		).isNotEmpty();
	}
}
