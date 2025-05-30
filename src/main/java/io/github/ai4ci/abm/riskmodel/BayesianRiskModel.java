//package io.github.ai4ci.abm.riskmodel;
//
//import org.immutables.value.Value;
//
//import io.github.ai4ci.abm.Person;
//
//@Value.Immutable
//public interface BayesianRiskModel extends RiskModel {
//
//	public static BayesianRiskModel init(Person state) {
//		return ImmutableBayesianRiskModel.builder().setEntity(state).build();
//	}
//
//	Kernel SYMPTOM_KERNEL = Kernel.from(4, 
//			0.1,0.2,0.4,0.8,
//			1,1,1,0.8,0.6,0.4,0.2);
//	
//	Kernel TEST_KERNEL = Kernel.from(6, 
//			0.1,0.2,0.4,0.6,0.8,0.9,
//			1,1,1,1,1,1,0.8,0.6,0.4,0.2);
//	
//	Kernel CONTACTS_KERNEL = Kernel.from(0,
//			1,1,1,1,0.8,0.6,0.4,0.2);
//	
//	
//	@Override
//	default Kernel getSymptomKernel() {
//		return SYMPTOM_KERNEL;
//	}
//
//	@Override
//	default Kernel getTestKernel() {
//		return TEST_KERNEL;
//	}
//
//	@Override
//	default Kernel getContactsKernel() {
//		return CONTACTS_KERNEL;
//	}
//
//	
//	
//}
