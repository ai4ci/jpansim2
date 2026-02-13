// src/test/java/io/github/ai4ci/RepositoryTest.java
package io.github.ai4ci.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.immutables.value.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.ai4ci.Import;
import io.github.ai4ci.util.Factor;
import io.github.ai4ci.util.Inversion;
import io.github.ai4ci.util.Repository;
import io.github.ai4ci.util.Repository.Indexed;
//import io.github.ai4ci.Data;


// @Data.Repository()
@Value.Style(
	deepImmutablesDetection = false,
	passAnnotations = {Import.class, Import.Id.class},
	get = {"is*", "get*"} // Detect 'get' and 'is' prefixes in accessor methods
)
public class RepositoryTest {

    @TempDir
    Path tempDir;

    private Path dataDir;
    
    @Value.Immutable
    @Import("city.csv")
    public static interface City extends Indexed<City> {
        @Import.Id String getId();
        String getName();
        Country getCountry(); // Foreign key to Country
        boolean isCapital();
        
        @Value.Lazy default Set<Person> getPeople() {
        	return this.find(Person.class, Person::getCity);
        }
        
    }
    
    @Value.Immutable
    @Import("country.csv")
    public static interface Country extends Indexed<Country> {
    	@Import.Id String getId();
        String getName();
    }
    
    @Value.Immutable
    @Import("person.csv")
    public interface Person  extends Indexed<Person> {
    	@Import.Id String getId();
        String getName();
        City getCity(); // Foreign key to City
        GivenGender getGivenGender();
    }
    
    public static enum GivenGender implements Factor {
		@Level("female") FEMALE,
		@Level("male") MALE,
		@Level("non binary") NON_BINARY
}

    @BeforeEach
    void setup() throws Exception {
        dataDir = tempDir.resolve("data");
        dataDir.toFile().mkdir();

        // Write test CSV files
        write(dataDir.resolve("country.csv"),
              "id,name",
              "1,France",
              "2,Germany",
              "3,Italy");

        write(dataDir.resolve("city.csv"),
              "id,name,country,capital",
              "101,Paris,1,TRUE",
              "102,Marseille,1,FALSE",
              "103,Berlin,2,TRUE",
              "104,Hamburg,2,FALSE",
              "105,Rome,3,TRUE");

        write(dataDir.resolve("person.csv"),
              "id,name,city,givenGender",
              "1001,Alice,101,female",
              "1002,Bob,103,male",
              "1003,Charlie,101,female",
              "1004,Diana,102,non binary");
    }

    private void write(Path file, String... lines) throws Exception {
        java.nio.file.Files.write(file, List.of(lines));
    }

    @Test
    void testLoadAndLink() throws Exception {
        
        Repository repo = Repository.loadAll(dataDir, Person.class, City.class, Country.class);

        // Check counts
        assertEquals(3, repo.streamValues(Country.class).count());
        assertEquals(5, repo.streamValues(City.class).count());
        assertEquals(4, repo.streamValues(Person.class).count());

        // Resolve: Alice -> City -> Country
        Person alice = repo.getValue(Person.class, "1001");
        City aliceCity = alice.getCity();
        Country aliceCountry = aliceCity.getCountry();

        assertEquals("Alice", alice.getName());
        assertEquals("Paris", aliceCity.getName());
        assertEquals("France", aliceCountry.getName());

        // Bob in Berlin → Germany
        Person bob = repo.getValue(Person.class, "1002");
        City bobCity = bob.getCity();
        Country bobCountry = bobCity.getCountry();

        assertEquals("Bob", bob.getName());
        assertEquals(GivenGender.MALE, bob.getGivenGender());
        assertEquals("Berlin", bobCity.getName());
        assertEquals("Germany", bobCountry.getName());
        
        System.out.println("Lookup by name");
        City paris = repo.findOne("Paris", City.class, City::getName);
        assertEquals(aliceCity, paris);
        
        System.out.println("Reverse lookup: parisiens");
        Set<Person> people = paris.getPeople();
        people.forEach(System.out::println);
        
        System.out.println("By Gender: males");
        repo.findValues(GivenGender.MALE, Person.class, Person::getGivenGender)
        	.forEach(System.out::println);
        
        City berlin = repo.findOne("Berlin", City.class, City::getName);
        Person diana = repo.findOne("Diana", Person.class, Person::getName);
        City mars = diana.getCity();
        
        assertEquals(berlin.getIndex(), mars.getIndex());
        System.out.println(repo.toString());
    }
    
    @Test
	void testCache() {
		var cache = Inversion.<Integer,Integer>cache(x -> x % 10, IntStream.range(0, 100).boxed());
		IntStream.range(0, 9).boxed().map(cache).forEach(System.out::println);
	}
}