package org.tat.fni.api.domain.services.ProposalServices;

import java.util.ArrayList;
import java.util.Calendar;
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
import org.tat.fni.api.dto.endowmentLifeDTO.EndowmentLifeDTO;
import org.tat.fni.api.dto.endowmentLifeDTO.EndowmentLifeProposalInsuredPersonBeneficiariesDTO;
import org.tat.fni.api.dto.endowmentLifeDTO.EndowmentLifeProposalInsuredPersonDTO;
import org.tat.fni.api.exception.DAOException;
import org.tat.fni.api.exception.SystemException;

@Service
public class EndowmentLifeProposalService extends BaseService implements ILifeProductsProposalService {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LifeMedicalProposalRepository lifeMedicalProposalRepo;
	
	@Autowired
	private TownShipService townShipService;

	@Autowired
	private ICustomIdGenerator customId;

	@Autowired
	private ILifeProposalService lifeProposalService;

	@Value("${publicTermLifeProductId}")
	private String publicTermLifeProductId;
	
	@Value("${branchId}")
	private String branchId;

	@Value("${salespointId}")
	private String salespointId;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public <T> List<LifeMedicalProposal> createDtoToProposal(T proposalDto) {
		try {

			EndowmentLifeDTO publicLifeDTO = (EndowmentLifeDTO) proposalDto;

			List<LifeMedicalProposal> publicLifeProposalList = convertProposalDTOToProposal(publicLifeDTO);
			lifeMedicalProposalRepo.saveAll(publicLifeProposalList);

			return publicLifeProposalList;

		} catch (DAOException e) {

			logger.error("JOEERROR:" + e.getMessage(), e);
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
	}

	@Override
	public <T> List<LifeMedicalProposal> convertProposalDTOToProposal(T proposalDto) {

		EndowmentLifeDTO publicLifeDTO = (EndowmentLifeDTO) proposalDto;

		List<LifeMedicalProposal> lifeProposalList = new ArrayList<>();

		try {
			publicLifeDTO.getProposalInsuredPersonList().forEach(insuredPerson -> {

				LifeMedicalProposal lifeProposal = new LifeMedicalProposal();

				LifeMedicalCustomer customer = lifeProposalService.checkCustomerAvailabilityTemp(publicLifeDTO.getCustomer());

				if (customer == null) {
					lifeProposal.setCustomer(lifeProposalService.createNewCustomer(publicLifeDTO.getCustomer()));
				} else {
					lifeProposal.setCustomer(customer);
				}

				lifeProposal.getProposalInsuredPersonList().add(createInsuredPerson(insuredPerson));

				lifeProposal.setComplete(false);
				lifeProposal.setStatus(false);
				lifeProposal.setProposalType(ProposalType.UNDERWRITING);
				lifeProposal.setSubmittedDate(publicLifeDTO.getSubmittedDate());
				lifeProposal.setPeriodMonth(publicLifeDTO.getPeriodMonth() / 12);
				lifeProposal.setSaleChannelType(SaleChannelType.AGENT);
				lifeProposal.setPaymentTypeId(publicLifeDTO.getPaymentTypeId());
				lifeProposal.setAgentId(publicLifeDTO.getAgentId());
				lifeProposal.setSalesPointsId(salespointId);
				lifeProposal.setBranchId(branchId);

				String proposalNo = customId.getNextId("PUBLICLIFE_PROPOSAL_NO", null);
				lifeProposal.setStartDate(publicLifeDTO.getStartDate());
				
				Calendar cal = Calendar.getInstance();
				cal.setTime(lifeProposal.getStartDate());
				cal.add(Calendar.YEAR, lifeProposal.getPeriodMonth());
				
				lifeProposal.setEndDate(cal.getTime());
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
		try {

			EndowmentLifeProposalInsuredPersonDTO dto = (EndowmentLifeProposalInsuredPersonDTO) proposalInsuredPersonDTO;

			Optional<Township> townshipOptional = townShipService.findById(dto.getTownshipId());
			
			ResidentAddress residentAddress = new ResidentAddress();
			residentAddress.setResidentAddress(dto.getResidentAddress());
			residentAddress.setTownship(townshipOptional.get());

			Name name = new Name();
			name.setFirstName(dto.getFirstName());
			name.setMiddleName(dto.getMiddleName());
			name.setLastName(dto.getLastName());

			LifeMedicalInsuredPerson insuredPerson = new LifeMedicalInsuredPerson();
			insuredPerson.setProductId(publicTermLifeProductId);
			insuredPerson.setInitialId(dto.getInitialId());
			insuredPerson.setProposedSumInsured(dto.getProposedSumInsured());
			insuredPerson.setIdType(IdType.valueOf(dto.getIdType()));
			insuredPerson.setIdNo(dto.getIdNo());
			insuredPerson.setFatherName(dto.getFatherName());
			insuredPerson.setDateOfBirth(dto.getDateOfBirth());
			insuredPerson.setAge(DateUtils.getAgeForNextYear(dto.getDateOfBirth()));
			insuredPerson.setGender(Gender.valueOf(dto.getGender()));
			insuredPerson.setResidentAddress(residentAddress);
			insuredPerson.setName(name);
			insuredPerson.setOccupationId(dto.getOccupationID());
			insuredPerson.setPhone(dto.getPhone());

			String insPersonCodeNo = customId.getNextId("LIFE_INSUREDPERSON_CODENO", null);
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

			EndowmentLifeProposalInsuredPersonBeneficiariesDTO dto = (EndowmentLifeProposalInsuredPersonBeneficiariesDTO) insuredPersonBeneficiariesDto;

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
			beneficiary.setPhone(dto.getPhone());
			beneficiary.setIdType(IdType.valueOf(dto.getIdType()));
			beneficiary.setIdNo(dto.getIdNo());
			beneficiary.setGender(Gender.valueOf(dto.getGender()));
			beneficiary.setResidentAddress(residentAddress);
			beneficiary.setAge(dto.getAge());
			beneficiary.setName(name);
			beneficiary.setProposalInsuredPerson(insuredPerson);
			beneficiary.setRelationshipId(dto.getRelationshipId());

			String beneficiaryNo = customId.getNextId("LIFE_BENEFICIARY_NO", null);
			beneficiary.setBeneficiaryNo(beneficiaryNo);

			return beneficiary;

		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), e.getMessage());
		}
	}

}
