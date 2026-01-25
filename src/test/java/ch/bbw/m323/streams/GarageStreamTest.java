package ch.bbw.m323.streams;

import java.io.IOException;
import java.util.List;

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

	@BeforeEach
	void readJson() throws IOException {
		// TODO: change to "manynull.json" for a harder experience
		try (var in = GarageStreamTest.class.getClassLoader().getResourceAsStream("fewnull.json")) {
			inventory = new ObjectMapper().readValue(in, Inventory.class);
		}
	}

	@Test
	void aTest() {

	}
}
