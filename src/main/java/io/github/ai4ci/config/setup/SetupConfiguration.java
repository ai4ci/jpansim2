package io.github.ai4ci.config.setup;

import java.io.Serializable;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction;



@Value.Immutable
@JsonSerialize(as = ImmutableSetupConfiguration.class)
@JsonDeserialize(as = ImmutableSetupConfiguration.class)
public interface SetupConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable {

	public ImmutableSetupConfiguration DEFAULT = ImmutableSetupConfiguration.builder()
			.setName("default")
			.setInitialImports(30)
			.setDemographics(LocationAwareDemography.DEFAULT)
			.setNetwork(ErdosReyniConfiguration.DEFAULT)
			.build();
	
	Integer getInitialImports();
	NetworkConfiguration getNetwork();
	DemographicConfiguration getDemographics();
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialSetupConfiguration.class)
	@JsonDeserialize(as = PartialSetupConfiguration.class)
	public interface _PartialSetupConfiguration extends SetupConfiguration, Abstraction.Modification<SetupConfiguration> {
		default _PartialSetupConfiguration self() {return this;}
	}
	
	@JsonIgnore
	default SmallHilbertCurve hilbertBits() {
		double size = (double) this.getNetwork().getNetworkSize();
		int bits = (int) Math.ceil(0.5*Math.log(size)/Math.log(2));
		return  HilbertCurve.small().bits(bits).dimensions(2);
	}
	
	@JsonIgnore
	default double[] getHilbertCoords(Integer id) {
		double size = (double) this.getNetwork().getNetworkSize();
		var hilbert = hilbertBits();
		long[] tmp =  hilbert.point((long) (id/size*Math.pow(2, hilbert.bits()*2)));
		return new double[] {
			((double) tmp[0])/Math.pow(2, hilbert.bits()),
			((double) tmp[1])/Math.pow(2, hilbert.bits()),
		};
	};
	
	
	
}