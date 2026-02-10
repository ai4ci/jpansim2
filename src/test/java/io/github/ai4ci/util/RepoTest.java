package io.github.ai4ci.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.config.refdata.UKCensus;
import io.github.ai4ci.config.refdata.UKCensus.Geography;
import io.github.ai4ci.util.Repository.FunctionKey;

class RepoTest {

	@Test
	void repositoryKeyTest() {
		
		// Passes
		var key1 = FunctionKey.<Geography,String>create( Geography::getId );
		var key2 = FunctionKey.<Geography,String>create( Geography::getId );
		assertEquals(key1,  key2);
		
		// Fails due to the fact that these expressions get serialised very slightly
		// differently. (usually one byte difference).
//		var key3 = FunctionKey.<Geography,String>create( g -> g.getId() );
//		var key4 = FunctionKey.<Geography,String>create( g -> g.getId() );
//		assertEquals(key3,  key4);
	}
	
	@Test
	void test() {
		var repo = UKCensus.load();
		repo.streamValues(UKCensus.Commuting.class).forEach(System.out::println);
		
	}

}
