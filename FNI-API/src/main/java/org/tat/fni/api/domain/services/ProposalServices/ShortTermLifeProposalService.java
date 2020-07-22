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
import org.tat.fni.api.dto.shortTermEndowmentLifeDTO.ShortTermEndowmentLifeDTO;
import org.tat.fni.api.dto.shortTermEndowmentLifeDTO.ShortTermProposalInsuredPersonBeneficiariesDTO;
import org.tat.fni.api.dto.shortTermEndowmentLifeDTO.ShortTermProposalInsuredPersonDTO;
import org.tat.fni.api.exception.DAOException;
import org.tat.fni.api.exception.SystemException;

@Service
public class ShortTermLifeProposalService extends BaseService implements ILifeProductsProposalService {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LifeMedicalProposalRepository lifeMedicalProposalRepo;

	@Autowired
	private TownShipService townShipService;

	@Autowired
	private ICustomIdGenerator customIdRepo;

	@Autowired
	private ILifeProposalService lifeProposalService;

	@Value("${branchId}")
	private String branchId;

	@Value("${salespointId}")
	private String salespointId;

	@Value("${shorttermLifeProductId}")
	private String shorttermLifeProductId;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public <T> List<LifeMedicalProposal> createDtoToProposal(T proposalDto) {
		try {

			ShortTermEndowmentLifeDTO shortTermEndowmentLifeDto = (ShortTermEndowmentLifeDTO) proposalDto;

			// convert shortTermEndowmentlifeProposalDTO to lifeproposal
			List<LifeMedicalProposal> shortTermEndowmentLifeProposalList = convertProposalDTOToProposal(
					shortTermEndowmentLifeDto);

			shortTermEndowmentLifeProposalList = lifeMedicalProposalRepo.saveAll(shortTermEndowmentLifeProposalList);

//			String id = DateUtils.formattedSqlDate(new Date())
//					.concat(shortTermEndowmentLifeProposalList.get(0).getProposalNo());
//			String referenceNo = shortTermEndowmentLifeProposalList.get(0).getId();
//			String referenceType = "SHORT_ENDOWMENT_LIFE";
//			String createdDate = DateUtils.formattedSqlDate(new Date());
//			String workflowDate = DateUtils.formattedSqlDate(new Date());
//
//			lifeProposalRepo.saveToWorkflow(id, referenceNo, referenceType, createdDate);
//			lifeProposalRepo.saveToWorkflowHistory(id, referenceNo, referenceType, createdDate, workflowDate);

			return shortTermEndowmentLifeProposalList;
			
		} catch (Exception e) {
			logger.error("JOEERROR:" + e.getMessage(), e);
			throw e;
		}
	}

	// ForshortTermEndowmentlifeDto to proposal
	@Override
	public <T> List<LifeMedicalProposal> convertProposalDTOToProposal(T proposalDto) {
		
		List<LifeMedicalProposal> lifeProposalList = new ArrayList<>();
		ShortTermEndowmentLifeDTO shortTermEndowmentLifeDto = (ShortTermEndowmentLifeDTO) proposalDto;
		
		try {
			shortTermEndowmentLifeDto.getProposalInsuredPersonList().forEach(insuredPerson -> {

				LifeMedicalProposal lifeProposal = new LifeMedicalProposal();

				LifeMedicalCustomer customer = lifeProposalService
						.checkCustomerAvailabilityTemp(shortTermEndowmentLifeDto.getCustomer());

				if (customer == null) {
					lifeProposal.setCustomer(lifeProposalService.createNewCustomer(shortTermEndowmentLifeDto.getCustomer()));
				} else {
					lifeProposal.setCustomer(customer);
				}

//				lifeProposalService.setPeriodMonthForKeyFacterValue(shortTermEndowmentLifeDto.getPeriodMonth(),
//						shortTermEndowmentLifeDto.getPaymentTypeId());

				lifeProposal.getProposalInsuredPersonList().add(createInsuredPerson(insuredPerson));

				lifeProposal.setComplete(false);
				lifeProposal.setStatus(false);
				lifeProposal.setProposalType(ProposalType.UNDERWRITING);
				lifeProposal.setSubmittedDate(shortTermEndowmentLifeDto.getSubmittedDate());
				lifeProposal.setPaymentTypeId(shortTermEndowmentLifeDto.getPaymentTypeId());
				lifeProposal.setAgentId(shortTermEndowmentLifeDto.getAgentId());
				lifeProposal.setSalesPointsId(salespointId);
				lifeProposal.setBranchId(branchId);

				String proposalNo = customIdRepo.getNextId("SHORT_ENDOWMENT_PROPOSAL_NO", null);
				lifeProposal.setStartDate(shortTermEndowmentLifeDto.getStartDate());
				lifeProposal.setPeriodMonth(shortTermEndowmentLifeDto.getPeriodMonth());
				lifeProposal.setSaleChannelType(SaleChannelType.AGENT);
				lifeProposal.setEndDate(shortTermEndowmentLifeDto.getEndDate());
				lifeProposal.setProposalNo(proposalNo);

//				lifeProposal = lifeProposalService.calculatePremium(lifeProposal);
//				lifeProposalService.calculateTermPremium(lifeProposal);

				lifeProposalList.add(lifeProposal);

			});
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
		return lifeProposalList;
	}

	@Override
	public <T> LifeMedicalInsuredPerson createInsuredPerson(T proposalInsuredPersonDTO) {
		try {

			ShortTermProposalInsuredPersonDTO dto = (ShortTermProposalInsuredPersonDTO) proposalInsuredPersonDTO;

			ResidentAddress residentAddress = new ResidentAddress();
			residentAddress.setResidentAddress(dto.getResidentAddress());

			Name name = new Name();
			name.setFirstName(dto.getFirstName());
			name.setMiddleName(dto.getMiddleName());
			name.setLastName(dto.getLastName());

			LifeMedicalInsuredPerson insuredPerson = new LifeMedicalInsuredPerson();

			insuredPerson.setInitialId(dto.getInitialId());
			insuredPerson.setProposedSumInsured(dto.getProposedSumInsured());
			insuredPerson.setProposedPremium(dto.getProposedPremium());
			insuredPerson.setIdType(IdType.valueOf(dto.getIdType()));
			insuredPerson.setIdNo(dto.getIdNo());
			insuredPerson.setFatherName(dto.getFatherName());
			insuredPerson.setDateOfBirth(dto.getDateOfBirth());
			insuredPerson.setAge(DateUtils.getAgeForNextYear(dto.getDateOfBirth()));
			insuredPerson.setGender(Gender.valueOf(dto.getGender()));
			insuredPerson.setResidentAddress(residentAddress);
			insuredPerson.setName(name);
			insuredPerson.setOccupationId(dto.getOccupationID());
			insuredPerson.setProductId(shorttermLifeProductId);

			String insPersonCodeNo = customIdRepo.getNextId("LIFE_INSUREDPERSON_CODENO", null);
			insuredPerson.setInsPersonCodeNo(insPersonCodeNo);

//			insuredPerson.getProduct().getKeyFactorList().forEach(keyfactor -> {
//				insuredPerson.getKeyFactorValueList()
//						.add(lifeProposalService.createKeyFactorValue(keyfactor, insuredPerson, dto));
//			});

			dto.getInsuredPersonBeneficiariesList().forEach(beneficiary -> {
				insuredPerson.getInsuredPersonBeneficiariesList()
						.add(createInsuredPersonBeneficiareis(beneficiary, insuredPerson));
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

			ShortTermProposalInsuredPersonBeneficiariesDTO dto = (ShortTermProposalInsuredPersonBeneficiariesDTO) insuredPersonBeneficiariesDto;

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
			beneficiary.setProposalInsuredPerson(insuredPerson);
			beneficiary.setGender(Gender.valueOf(dto.getGender()));
			beneficiary.setResidentAddress(residentAddress);
			beneficiary.setName(name);
			beneficiary.setRelationshipId(dto.getRelationshipId());
			
			String beneficiaryNo = customIdRepo.getNextId("LIFE_BENEFICIARY_NO", null);
			beneficiary.setBeneficiaryNo(beneficiaryNo);
			
			return beneficiary;
			
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
	}

}
