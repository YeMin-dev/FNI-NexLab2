package org.tat.fni.api.domain.services.Interfaces;

import org.tat.fni.api.common.KeyFactor;
import org.tat.fni.api.domain.MedicalKeyFactorValue;
import org.tat.fni.api.domain.MedicalProposal;
import org.tat.fni.api.domain.MedicalProposalInsuredPerson;
import org.tat.fni.api.domain.proposalTemp.LifeMedicalCustomer;
import org.tat.fni.api.dto.customerDTO.CustomerDto;

public interface IMedicalProposalService {
	
	public <T> MedicalKeyFactorValue createKeyFactorValue
	(KeyFactor keyfactor, MedicalProposalInsuredPerson insuredPerson, T dto);
	
	public void setPeriodMonthForKeyFacterValue(int periodMonth, String paymentTypeId);
	
	public MedicalProposal calculatePremium(MedicalProposal medicalProposal);
	
	public void calculateTermPremium(MedicalProposal medicalProposal);
	
	public <T> LifeMedicalCustomer createNewCustomer(T customerDto);
	
	public Boolean checkCustomerAvailability(CustomerDto dto);
	
	public LifeMedicalCustomer checkCustomerAvailabilityTemp(CustomerDto dto);

}
