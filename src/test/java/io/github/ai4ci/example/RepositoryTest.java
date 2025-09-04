// src/test/java/io/github/ai4ci/RepositoryTest.java
package io.github.ai4ci.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.immutables.value.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.ai4ci.Import;
import io.github.ai4ci.util.Repository;

public class RepositoryTest {

    @TempDir
    Path tempDir;

    private Path dataDir;
    
    @Value.Immutable
    @Import("city.csv")
    public static interface City {
        @Import.Id int getId();
        String getName();
        Country getCountry(); // Foreign key to Country
    }
    
    @Value.Immutable
    @Import("country.csv")
    public static interface Country {
    	@Import.Id int getId();
        String getName();
    }
    
    @Value.Immutable
    @Import("person.csv")
    public interface Person {
    	@Import.Id int getId();
        String getName();
        City getCity(); // Foreign key to City
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
              "id,name,country",
              "101,Paris,1",
              "102,Marseille,1",
              "103,Berlin,2",
              "104,Hamburg,2",
              "105,Rome,3");

        write(dataDir.resolve("person.csv"),
              "id,name,city",
              "1001,Alice,101",
              "1002,Bob,103",
              "1003,Charlie,105",
              "1004,Diana,102");
    }

    private void write(Path file, String... lines) throws Exception {
        java.nio.file.Files.write(file, List.of(lines));
    }

    @Test
    void testLoadAndLink() throws Exception {
//        Repository repo = new Repository(dataDir);
//
//        // Load in dependency order: Country → City → Person
//        repo.readCSV(Country.class);
//        repo.readCSV(City.class);
//        repo.readCSV(Person.class);
        
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
        assertEquals("Berlin", bobCity.getName());
        assertEquals("Germany", bobCountry.getName());
    }
}