//package io.github.ai4ci.abm.riskmodel;
//
//import org.immutables.value.Value;
//
//import io.github.ai4ci.abm.Person;
//
//@Value.Immutable
//public interface BinaryRiskModel extends RiskModel {
//
//	
//
//	public static BinaryRiskModel init(Person state) {
//		return ImmutableBinaryRiskModel.builder().setEntity(state).build();
//	}
//
//	
//	Kernel SYMPTOM_KERNEL = Kernel.from(4, 
//			1,1,1,1,
//			1,1,1,1,1,1,1,1);
//	
//	Kernel TEST_KERNEL = Kernel.from(6, 
//			1,1,1,1,1,1,
//			1,1,1,1,1,1,1,1,1,1);
//	
//	Kernel CONTACTS_KERNEL = Kernel.from(0,
//			1,1,1,1,1,1,1,1);
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
//}
