package io.github.ai4ci.config.refdata;

import org.immutables.value.Value;
import io.github.ai4ci.Import;
import io.github.ai4ci.util.Factor;
import io.github.ai4ci.util.Repository;
import io.github.ai4ci.util.Repository.Indexed;

import java.nio.file.Paths;

import javax.annotation.Nullable;

@Value.Style(
		deepImmutablesDetection = false,
		passAnnotations = {Import.class, Import.Id.class},
		get = {"is*", "get*"}
)
public class UKCensus {

		public static Repository load() {
				try {
						var path = Paths.get(UKCensus.class.getClassLoader().getResource("UKCensus").toURI());
						return Repository.loadAll(path, TYPES);
				} catch (Exception e) { throw new RuntimeException(e); }
		}


		public static final Class<?>[] TYPES = new Class<?>[] {
				AgeGender.class,
				Commuting.class,
				Distances.class,
				Ethnicity.class,
				Geography.class,
				Industry.class,
				Unemployment.class};

		public static enum CommutingType implements Factor {
				@Level("Bicycle") BICYCLE,
				@Level("Bus, minibus or coach") BUS_MINIBUS_OR_COACH,
				@Level("Driving a car or van") DRIVING_A_CAR_OR_VAN,
				@Level("Motorcycle, scooter or moped") MOTORCYCLE_SCOOTER_OR_MOPED,
				@Level("On foot") ON_FOOT,
				@Level("Other method of travel to work") OTHER_METHOD_OF_TRAVEL_TO_WORK,
				@Level("Passenger in a car or van") PASSENGER_IN_A_CAR_OR_VAN,
				@Level("Taxi") TAXI,
				@Level("Train") TRAIN,
				@Level("Underground, metro, light rail, tram") UNDERGROUND_METRO_LIGHT_RAIL_TRAM,
				@Level("Work mainly at or from home") WORK_MAINLY_AT_OR_FROM_HOME
		}

		public static enum Employment implements Factor {
				@Level("In employment") IN_EMPLOYMENT,
				@Level("Long-term sick or disabled") LONG_TERM_SICK_OR_DISABLED,
				@Level("Looking after home or family") LOOKING_AFTER_HOME_OR_FAMILY,
				@Level("Other") OTHER,
				@Level("Retired") RETIRED,
				@Level("Student") STUDENT,
				@Level("Unemployed") UNEMPLOYED
		}

		public static enum EmploymentType implements Factor {
				@Level("Employee") EMPLOYEE,
				@Level("Self-employed with employees") SELF_EMPLOYED_WITH_EMPLOYEES,
				@Level("Self-employed without employees") SELF_EMPLOYED_WITHOUT_EMPLOYEES
		}

		public static enum FullTime implements Factor {
				@Level("Full") FULL,
				@Level("Part") PART
		}

		public static enum Gender implements Factor {
				@Level("Female") FEMALE,
				@Level("Male") MALE
		}

		public static enum Group implements Factor {
				@Level("Asian, Asian British or Asian Welsh") ASIAN_ASIAN_BRITISH_OR_ASIAN_WELSH,
				@Level("Black, Black British, Black Welsh, Caribbean or African") BLACK_BLACK_BRITISH_BLACK_WELSH_CARIBBEAN_OR_AFRICAN,
				@Level("Mixed or Multiple ethnic groups") MIXED_OR_MULTIPLE_ETHNIC_GROUPS,
				@Level("Other ethnic group") OTHER_ETHNIC_GROUP,
				@Level("White") WHITE
		}

		public static enum IndustryName implements Factor {
				@Level("Accommodation and food service activities") ACCOMMODATION_AND_FOOD_SERVICE_ACTIVITIES,
				@Level("Administrative and support service activities") ADMINISTRATIVE_AND_SUPPORT_SERVICE_ACTIVITIES,
				@Level("Agriculture, Forestry and fishing") AGRICULTURE_FORESTRY_AND_FISHING,
				@Level("Construction") CONSTRUCTION,
				@Level("Education") EDUCATION,
				@Level("Electricity, gas, steam and air conditioning supply") ELECTRICITY_GAS_STEAM_AND_AIR_CONDITIONING_SUPPLY,
				@Level("Financial and insurance activities") FINANCIAL_AND_INSURANCE_ACTIVITIES,
				@Level("Human health and social work activities") HUMAN_HEALTH_AND_SOCIAL_WORK_ACTIVITIES,
				@Level("Information and communication") INFORMATION_AND_COMMUNICATION,
				@Level("Manufacturing") MANUFACTURING,
				@Level("Mining and quarrying") MINING_AND_QUARRYING,
				@Level("Professional, scientific and technical activities") PROFESSIONAL_SCIENTIFIC_AND_TECHNICAL_ACTIVITIES,
				@Level("Public administration and defence; compulsory social security") PUBLIC_ADMINISTRATION_AND_DEFENCE_COMPULSORY_SOCIAL_SECURITY,
				@Level("Real estate activities") REAL_ESTATE_ACTIVITIES,
				@Level("Transport and storage") TRANSPORT_AND_STORAGE,
				@Level("Water supply; Sewerage, Waste management and Remediation activities") WATER_SUPPLY_SEWERAGE_WASTE_MANAGEMENT_AND_REMEDIATION_ACTIVITIES,
				@Level("Wholesale and retail trade; repair of motor vehicles and motorcycles") WHOLESALE_AND_RETAIL_TRADE_REPAIR_OF_MOTOR_VEHICLES_AND_MOTORCYCLES
		}

		public static enum Student implements Factor {
				@Level("active (excluding full-time students)") ACTIVE_EXCLUDING_FULL_TIME_STUDENTS,
				@Level("active and a full-time student") ACTIVE_AND_A_FULL_TIME_STUDENT,
				@Level("inactive") INACTIVE
		}

		public static enum Subgroup implements Factor {
				@Level("African") AFRICAN,
				@Level("Any other ethnic group") ANY_OTHER_ETHNIC_GROUP,
				@Level("Arab") ARAB,
				@Level("Bangladeshi") BANGLADESHI,
				@Level("Caribbean") CARIBBEAN,
				@Level("Chinese") CHINESE,
				@Level("English, Welsh, Scottish, Northern Irish or British") ENGLISH_WELSH_SCOTTISH_NORTHERN_IRISH_OR_BRITISH,
				@Level("Gypsy or Irish Traveller") GYPSY_OR_IRISH_TRAVELLER,
				@Level("Indian") INDIAN,
				@Level("Irish") IRISH,
				@Level("Other Asian") OTHER_ASIAN,
				@Level("Other Black") OTHER_BLACK,
				@Level("Other Mixed or Multiple ethnic groups") OTHER_MIXED_OR_MULTIPLE_ETHNIC_GROUPS,
				@Level("Other White") OTHER_WHITE,
				@Level("Pakistani") PAKISTANI,
				@Level("Roma") ROMA,
				@Level("White and Asian") WHITE_AND_ASIAN,
				@Level("White and Black African") WHITE_AND_BLACK_AFRICAN,
				@Level("White and Black Caribbean") WHITE_AND_BLACK_CARIBBEAN
		}


		@Value.Immutable
		@Import("AgeGender.csv")
		public static interface AgeGender extends Indexed<AgeGender> {
				Geography getGeography();
				int getCount();
				int getTotal();
				double getAge();
				Gender getGender();
				@Import.Id String getId();
		}

		@Value.Immutable
		@Import("Commuting.csv")
		public static interface Commuting extends Indexed<Commuting> {
				Geography getGeography();
				int getCount();
				int getTotal();
				CommutingType getCommutingType();
				@Import.Id String getId();
		}

		@Value.Immutable
		@Import("Distances.csv")
		public static interface Distances extends Indexed<Distances> {
				Geography getSourceGeography();
				Geography getTargetGeography();
				double getDistance();
				@Import.Id String getId();
		}

		@Value.Immutable
		@Import("Ethnicity.csv")
		public static interface Ethnicity extends Indexed<Ethnicity> {
				Geography getGeography();
				int getCount();
				int getTotal();
				Group getGroup();
				@Nullable Subgroup getSubgroup();
				@Import.Id String getId();
		}

		@Value.Immutable
		@Import("Geography.csv")
		public static interface Geography extends Indexed<Geography> {
				@Import.Id String getId();
				String getName();
		}

		@Value.Immutable
		@Import("Industry.csv")
		public static interface Industry extends Indexed<Industry> {
				Geography getGeography();
				int getCount();
				int getTotal();
				IndustryName getIndustryName();
				@Import.Id String getId();
		}

		@Value.Immutable
		@Import("Unemployment.csv")
		public static interface Unemployment extends Indexed<Unemployment> {
				Geography getGeography();
				int getCount();
				int getTotal();
				Student getStudent();
				@Nullable Employment getEmployment();
				@Nullable EmploymentType getEmploymentType();
				@Nullable FullTime getFullTime();
				@Import.Id String getId();
		}
}
