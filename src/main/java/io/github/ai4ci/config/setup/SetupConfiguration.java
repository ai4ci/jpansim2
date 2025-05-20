package io.github.ai4ci.config.setup;

import java.io.Serializable;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.abm.mechanics.Abstraction;



// @JsonTypeInfo(use = Id.DEDUCTION)
@JsonTypeInfo(use = Id.SIMPLE_NAME)
// @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As..PROPERTY, property = "type")
@JsonSubTypes( {
	@Type(value = ImmutableWattsStrogatzConfiguration.class, name = "watts-strogatz"), 
	@Type(value = ImmutableAgeStratifiedNetworkConfiguration.class, name = "age-stratified") 
} )
public interface SetupConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable {

	public static interface Builder {
		Builder setReplicate(Integer value);
		Builder setName(String name);
		SetupConfiguration build();
	}
	
	SetupConfiguration withReplicate(Integer i);
	SetupConfiguration withName(String name);
	
	Integer getNetworkSize();
	Integer getInitialImports();
	
	@JsonIgnore
	@Value.Derived private int hilbertBits() {
		double size = (double) this.getNetworkSize();
		return (int) Math.ceil(0.5*Math.log(size)/Math.log(2));
	}
	
	default long[] getHilbertCoords(Integer id) {
		SmallHilbertCurve c = HilbertCurve.small().bits(hilbertBits()).dimensions(2);
		double size = (double) this.getNetworkSize();
		return c.point((long) (id/size*Math.pow(2, hilbertBits()*2)));
	};
}