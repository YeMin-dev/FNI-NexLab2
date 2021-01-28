package org.tat.fni.api.domain.services.ProposalServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tat.fni.api.common.Name;
import org.tat.fni.api.common.ResidentAddress;
import org.tat.fni.api.common.emumdata.Gender;
import org.tat.fni.api.common.emumdata.IdType;
import org.tat.fni.api.common.emumdata.ProposalType;
import org.tat.fni.api.common.emumdata.SaleChannelType;
import org.tat.fni.api.domain.DateUtils;
import org.tat.fni.api.domain.Township;
import org.tat.fni.api.domain.proposalTemp.LifeMedicalCustomer;
import org.tat.fni.api.domain.proposalTemp.LifeMedicalInsuredPerson;
import org.tat.fni.api.domain.proposalTemp.LifeMedicalInsuredPersonBeneficiary;
import org.tat.fni.api.domain.proposalTemp.LifeMedicalProposal;
import org.tat.fni.api.domain.proposalTemp.repository.LifeMedicalProposalRepository;
import org.tat.fni.api.domain.services.BaseService;
import org.tat.fni.api.domain.services.TownShipService;
import org.tat.fni.api.domain.services.Interfaces.ICustomIdGenerator;
import org.tat.fni.api.domain.services.Interfaces.ILifeProductsProposalService;
import org.tat.fni.api.domain.services.Interfaces.ILifeProposalService;
import org.tat.fni.api.dto.sportManDTO.SportManDTO;
import org.tat.fni.api.dto.sportManDTO.SportManInsuredPersonBeneficiariesDTO;
import org.tat.fni.api.dto.sportManDTO.SportManProposalInsuredPersonDTO;
import org.tat.fni.api.exception.DAOException;
import org.tat.fni.api.exception.SystemException;

@Service
public class SportManProposalService extends BaseService implements ILifeProductsProposalService {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LifeMedicalProposalRepository lifeMedicalProposalRepo;

	@Autowired
	private TownShipService townShipService;

	@Autowired
	private ICustomIdGenerator customIdRepo;

	@Autowired
	private ILifeProposalService lifeProposalService;

	@Value("${sportmanProductId}")
	private String sportmanProductId;
	
	@Value("${branchId}")
	private String branchId;

	@Value("${salespointId}")
	private String salespointId;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public <T> List<LifeMedicalProposal> createDtoToProposal(T proposalDto) {
		try {

			SportManDTO sportMandto = (SportManDTO) proposalDto;

			// convert SportManDTOToProposal to lifeproposal
			List<LifeMedicalProposal> sportmanproposalList = convertProposalDTOToProposal(sportMandto);
			lifeMedicalProposalRepo.saveAll(sportmanproposalList);

			return sportmanproposalList;
			
		} catch (Exception e) {
			logger.error("JOEERROR:" + e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public <T> List<LifeMedicalProposal> convertProposalDTOToProposal(T proposalDto) {

		List<LifeMedicalProposal> lifeProposalList = new ArrayList<>();
		SportManDTO sportMandto = (SportManDTO) proposalDto;
		
		try {
			sportMandto.getProposalInsuredPersonList().forEach(insuredPerson -> {
				
				LifeMedicalProposal lifeProposal = new LifeMedicalProposal();

				LifeMedicalCustomer customer = lifeProposalService.checkCustomerAvailabilityTemp(sportMandto.getCustomer());

				if (customer == null) {
					lifeProposal.setCustomer(lifeProposalService.createNewCustomer(sportMandto.getCustomer()));
				} else {
					lifeProposal.setCustomer(customer);
				}

//				lifeProposalService.setPeriodMonthForKeyFacterValue(sportMandto.getPeriodMonth(),
//						sportMandto.getPaymentTypeId());

				lifeProposal.getProposalInsuredPersonList().add(createInsuredPerson(insuredPerson));
				lifeProposal.setComplete(false);
				lifeProposal.setStatus(false);
				lifeProposal.setProposalType(ProposalType.UNDERWRITING);
				lifeProposal.setSubmittedDate(sportMandto.getSubmittedDate());
				lifeProposal.setPaymentTypeId(sportMandto.getPaymentTypeId());
				lifeProposal.setAgentId(sportMandto.getAgentId());
				lifeProposal.setSalesPointsId(salespointId);
				lifeProposal.setBranchId(branchId);
				lifeProposal.setOrganizationId(sportMandto.getOrganizationId());


				String proposalNo = customIdRepo.getNextId("LIFE_SPORTMAN_PROPOSAL_NO", null);
				lifeProposal.setStartDate(sportMandto.getStartDate());
				lifeProposal.setEndDate(sportMandto.getEndDate());
				lifeProposal.setSaleChannelType(SaleChannelType.WALKIN);
				lifeProposal.setPeriodMonth(sportMandto.getPeriodMonth() / 12);
				lifeProposal.setProposalNo(proposalNo);

				lifeProposalList.add(lifeProposal);
				
			});
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
		return lifeProposalList;
	}

	@Override
	public <T> LifeMedicalInsuredPerson createInsuredPerson(T proposalInsuredPersonDTO) {

		SportManProposalInsuredPersonDTO dto = (SportManProposalInsuredPersonDTO) proposalInsuredPersonDTO;

		try {
			Optional<Township> townshipOptional = townShipService.findById(dto.getTownshipId());
			
			ResidentAddress residentAddress = new ResidentAddress();
			residentAddress.setResidentAddress(dto.getResidentAddress());
			residentAddress.setTownship(townshipOptional.get());
			
			Name name = new Name();
			name.setFirstName(dto.getFirstName());
			name.setMiddleName(dto.getMiddleName());
			name.setLastName(dto.getLastName());

			LifeMedicalInsuredPerson insuredPerson = new LifeMedicalInsuredPerson();

			insuredPerson.setProductId(sportmanProductId);
			insuredPerson.setInitialId(dto.getInitialId());
			insuredPerson.setUnit(dto.getUnit());
			insuredPerson.setIdType(IdType.valueOf(dto.getIdType()));
			insuredPerson.setIdNo(dto.getIdNo());
			insuredPerson.setFatherName(dto.getFatherName());
			insuredPerson.setDateOfBirth(dto.getDateOfBirth());
			insuredPerson.setAge(DateUtils.getAgeForNextYear(dto.getDateOfBirth()));
			insuredPerson.setGender(Gender.valueOf(dto.getGender()));
			insuredPerson.setResidentAddress(residentAddress);
			insuredPerson.setName(name);
			insuredPerson.setOccupationId(null);
			insuredPerson.setRiskyOccupationId(null);
			insuredPerson.setTypesOfSportId(dto.getTypeofSportId());

			String insPersonCodeNo = customIdRepo.getNextId("LIFE_INSUREDPERSON_CODENO", null);
			insuredPerson.setInsPersonCodeNo(insPersonCodeNo);

			dto.getInsuredPersonBeneficiariesList().forEach(beneficiary -> {
				insuredPerson.getInsuredPersonBeneficiariesList().add(createInsuredPersonBeneficiareis(beneficiary, insuredPerson));
			});

			return insuredPerson;
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
	}

	@Override
	public <T> LifeMedicalInsuredPersonBeneficiary createInsuredPersonBeneficiareis(T insuredPersonBeneficiariesDto,
			LifeMedicalInsuredPerson insuredPerson) {
		try {

			SportManInsuredPersonBeneficiariesDTO dto = (SportManInsuredPersonBeneficiariesDTO) insuredPersonBeneficiariesDto;

			Optional<Township> townshipOptional = townShipService.findById(dto.getTownshipId());
			
			ResidentAddress residentAddress = new ResidentAddress();
			residentAddress.setResidentAddress(dto.getResidentAddress());
			residentAddress.setTownship(townshipOptional.get());
			
			Name name = new Name();
			name.setFirstName(dto.getFirstName());
			name.setMiddleName(dto.getMiddleName());
			name.setLastName(dto.getLastName());

			LifeMedicalInsuredPersonBeneficiary beneficiary = new LifeMedicalInsuredPersonBeneficiary();
			beneficiary.setInitialId(dto.getInitialId());
			beneficiary.setPercentage(dto.getPercentage());
			beneficiary.setIdType(IdType.valueOf(dto.getIdType()));
			beneficiary.setIdNo(dto.getIdNo());
			beneficiary.setResidentAddress(residentAddress);
			beneficiary.setName(name);
			beneficiary.setProposalInsuredPerson(insuredPerson);
			beneficiary.setRelationshipId(dto.getRelationshipId());
			
			String beneficiaryNo = customIdRepo.getNextId("LIFE_BENEFICIARY_NO", null);
			beneficiary.setBeneficiaryNo(beneficiaryNo);
			
			return beneficiary;
			
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
	}

}
